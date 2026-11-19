package com.kokolog.app

import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Room を使わない純粋な Repository テスト（DB の移行テストは別途）。 */
class ReportEntitlementRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-18T00:00:00Z"), ZoneOffset.UTC)
    private val july = YearMonth.of(2026, 7)
    private val august = YearMonth.of(2026, 8)

    private fun report(month: YearMonth, totalCount: Int = 30, canOffer: Boolean = true): ReportResult {
        val base = ReportAnalyzer.analyze(
            emptyList(),
            emptyList(),
            ReportPeriod.month(month.year, month.monthValue, TimeZone.getTimeZone("UTC"))
        )
        // Repository には Analyzer が出した準備判定を受け渡す。
        // この単体テストではその境界を独立に検証するため値のみ差し替える。
        return base.copy(
            summary = base.summary.copy(totalCount = totalCount),
            readiness = base.readiness.copy(canOfferMonthlyDetail = canOffer)
        )
    }

    @Test fun firstUnlockSucceedsAndSameMonthCannotConsumeTwice() = runBlocking {
        val dao = FakeDao()
        val repo = RoomReportEntitlementRepository(dao, clock)
        assertFalse(repo.hasUsedFreeTrial())
        assertEquals(FirstFreeTrialResult.GRANTED, repo.grantFirstFreeTrial(report(july)))
        assertEquals(MonthlyDetailAccessStatus.FREE_TRIAL_UNLOCKED, repo.accessForMonth(july))
        assertEquals(FirstFreeTrialResult.ALREADY_UNLOCKED, repo.grantFirstFreeTrial(report(july)))
        assertEquals(1, dao.getAll().size)
        assertTrue(repo.hasUsedFreeTrial())
    }

    @Test fun freeTrialCannotBeRedeemedForAnotherMonth() = runBlocking {
        val repo = RoomReportEntitlementRepository(FakeDao(), clock)
        assertEquals(FirstFreeTrialResult.GRANTED, repo.grantFirstFreeTrial(report(july)))
        assertEquals(FirstFreeTrialResult.TRIAL_ALREADY_USED, repo.grantFirstFreeTrial(report(august)))
        assertEquals(MonthlyDetailAccessStatus.LOCKED, repo.accessForMonth(august))
    }

    @Test fun ineligibleMonthIsNotRedeemable() = runBlocking {
        val repo = RoomReportEntitlementRepository(FakeDao(), clock)
        assertEquals(FirstFreeTrialResult.NOT_ELIGIBLE, repo.grantFirstFreeTrial(report(july, totalCount = 0)))
        assertEquals(FirstFreeTrialResult.NOT_ELIGIBLE, repo.grantFirstFreeTrial(report(july, canOffer = false)))
        assertEquals(FirstFreeTrialResult.NOT_ELIGIBLE, repo.grantFirstFreeTrial(report(YearMonth.of(2026, 11))))
        val weekly = report(july).copy(period = ReportPeriod.weekContaining(Instant.parse("2026-07-15T12:00:00Z").toEpochMilli(), TimeZone.getTimeZone("UTC")))
        assertEquals(FirstFreeTrialResult.NOT_ELIGIBLE, repo.grantFirstFreeTrial(weekly))
        assertFalse(repo.hasUsedFreeTrial())
    }

    @Test fun paidRecordIsNeverUnlockedByLocalHistoryAlone() = runBlocking {
        val dao = FakeDao()
        dao.insertIfAbsent(ReportEntitlementEntity(
            reportMonth = july.toString(),
            unlockMethod = ReportEntitlementEntity.PLAY_PURCHASE,
            grantedAt = clock.millis(),
            status = ReportEntitlementEntity.ACTIVE,
            playProductId = "monthly_report_2026_07",
            purchaseToken = "test-only-token"
        ))
        val repo = RoomReportEntitlementRepository(dao, clock)
        assertEquals(MonthlyDetailAccessStatus.PAYMENT_REQUIRES_VERIFICATION, repo.accessForMonth(july))
        assertEquals(FirstFreeTrialResult.MONTH_ALREADY_ASSIGNED, repo.grantFirstFreeTrial(report(july)))
        assertFalse(repo.hasUsedFreeTrial())
    }

    @Test fun revokedTrialStaysConsumedWithoutGrantingAccess() = runBlocking {
        val dao = FakeDao()
        dao.insertIfAbsent(ReportEntitlementEntity(
            reportMonth = july.toString(),
            unlockMethod = ReportEntitlementEntity.FREE_TRIAL,
            grantedAt = clock.millis(),
            status = ReportEntitlementEntity.REVOKED
        ))
        val repo = RoomReportEntitlementRepository(dao, clock)
        assertTrue(repo.hasUsedFreeTrial())
        assertEquals(MonthlyDetailAccessStatus.LOCKED, repo.accessForMonth(july))
        assertEquals(FirstFreeTrialResult.TRIAL_ALREADY_USED, repo.grantFirstFreeTrial(report(august)))
    }

    private class FakeDao : ReportEntitlementDao {
        private val items = linkedMapOf<String, ReportEntitlementEntity>()
        override suspend fun getByMonth(reportMonth: String) = items[reportMonth]
        override suspend fun getAll(): List<ReportEntitlementEntity> = items.values.toList()
        override suspend fun hasUsedFreeTrial(): Boolean =
            items.values.any { it.unlockMethod == ReportEntitlementEntity.FREE_TRIAL }
        override suspend fun insertIfAbsent(item: ReportEntitlementEntity): Long {
            if (items.containsKey(item.reportMonth)) return -1L
            items[item.reportMonth] = item
            return items.size.toLong()
        }
    }
}
