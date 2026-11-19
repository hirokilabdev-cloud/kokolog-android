package com.kokolog.app

import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.TimeZone

/**
 * 画面向けの権限状態。PAYMENT_REQUIRES_VERIFICATION は「閲覧可」ではない。
 * Google Play の購入照会が実装されるまでは、有料購入履歴だけで解放しない。
 */
enum class MonthlyDetailAccessStatus {
    LOCKED,
    FREE_TRIAL_UNLOCKED,
    PAYMENT_REQUIRES_VERIFICATION
}

enum class FirstFreeTrialResult {
    GRANTED,
    ALREADY_UNLOCKED,
    TRIAL_ALREADY_USED,
    MONTH_ALREADY_ASSIGNED,
    NOT_ELIGIBLE
}

/**
 * UI から DAO や Billing を直接参照させないためのインターフェース。
 * 将来、サーバー方式に移行しても画面側の API を維持できる。
 */
interface ReportEntitlementRepository {
    suspend fun accessForMonth(month: YearMonth): MonthlyDetailAccessStatus
    suspend fun hasUsedFreeTrial(): Boolean
    suspend fun grantFirstFreeTrial(report: ReportResult): FirstFreeTrialResult
}

/**
 * STEP5.1-B: ログインなし・Room 管理の閲覧権限。
 * - 無料解放: DAO の @Transaction により二重使用を防止。
 * - 有料購入: 現段階では照会・解放しない。STEP5.2 の Billing 連携で対応。
 * - 画面との接続: STEP5.1-C で実施。現行の無料レポート表示は変更しない。
 */
class RoomReportEntitlementRepository(
    private val dao: ReportEntitlementDao,
    private val clock: Clock = Clock.systemDefaultZone()
) : ReportEntitlementRepository {

    override suspend fun accessForMonth(month: YearMonth): MonthlyDetailAccessStatus {
        val entitlement = dao.getByMonth(month.toString())
            ?: return MonthlyDetailAccessStatus.LOCKED
        if (entitlement.status != ReportEntitlementEntity.ACTIVE) {
            return MonthlyDetailAccessStatus.LOCKED
        }
        return when (entitlement.unlockMethod) {
            ReportEntitlementEntity.FREE_TRIAL -> MonthlyDetailAccessStatus.FREE_TRIAL_UNLOCKED
            ReportEntitlementEntity.PLAY_PURCHASE -> MonthlyDetailAccessStatus.PAYMENT_REQUIRES_VERIFICATION
            else -> MonthlyDetailAccessStatus.LOCKED
        }
    }

    override suspend fun hasUsedFreeTrial(): Boolean = dao.hasUsedFreeTrial()

    override suspend fun grantFirstFreeTrial(report: ReportResult): FirstFreeTrialResult {
        val month = eligibleClosedMonth(report) ?: return FirstFreeTrialResult.NOT_ELIGIBLE
        val key = month.toString()
        val existing = dao.getByMonth(key)
        if (existing != null) return resultForExisting(existing)
        if (dao.hasUsedFreeTrial()) return FirstFreeTrialResult.TRIAL_ALREADY_USED

        // 最終的な競合防止は DAO の @Transaction に任せる。
        if (dao.grantFirstFreeTrial(key, clock.millis())) return FirstFreeTrialResult.GRANTED

        // 別操作で同時に権限が作られたケースを判定する。
        val after = dao.getByMonth(key)
        return if (after != null) resultForExisting(after)
        else FirstFreeTrialResult.TRIAL_ALREADY_USED
    }

    private fun resultForExisting(item: ReportEntitlementEntity): FirstFreeTrialResult =
        if (item.unlockMethod == ReportEntitlementEntity.FREE_TRIAL &&
            item.status == ReportEntitlementEntity.ACTIVE
        ) {
            FirstFreeTrialResult.ALREADY_UNLOCKED
        } else {
            FirstFreeTrialResult.MONTH_ALREADY_ASSIGNED
        }

    private fun eligibleClosedMonth(report: ReportResult): YearMonth? {
        if (report.period.type != ReportType.MONTH ||
            report.summary.totalCount <= 0 ||
            !report.readiness.canOfferMonthlyDetail
        ) return null

        val zone = runCatching { ZoneId.of(report.period.timeZoneId) }.getOrNull()
            ?: return null
        val start = Instant.ofEpochMilli(report.period.startMillis).atZone(zone)
        val month = YearMonth.from(start)
        if (month >= YearMonth.now(clock)) return null

        // 月次の全期間を扱う ReportAnalyzer の結果だけを受け付ける。
        val expected = ReportPeriod.month(
            month.year,
            month.monthValue,
            TimeZone.getTimeZone(zone)
        )
        if (report.period.startMillis != expected.startMillis ||
            report.period.endExclusiveMillis != expected.endExclusiveMillis
        ) return null
        return month
    }
}
