package com.kokolog.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import java.time.YearMonth

@Dao
interface ReportEntitlementDao {

    @Query("SELECT * FROM report_entitlements WHERE reportMonth = :reportMonth LIMIT 1")
    suspend fun getByMonth(reportMonth: String): ReportEntitlementEntity?

    @Query("SELECT * FROM report_entitlements ORDER BY reportMonth DESC")
    suspend fun getAll(): List<ReportEntitlementEntity>

    // REVOKEDになっても「初回無料を使用した」という履歴を消さない。
    @Query("SELECT EXISTS(SELECT 1 FROM report_entitlements WHERE unlockMethod = 'FREE_TRIAL')")
    suspend fun hasUsedFreeTrial(): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(item: ReportEntitlementEntity): Long

    /**
     * 初回無料を原子的に1回だけ付与。
     * 対象月に十分な分析があるかの判定は、呼び出し側で ReportAnalyzer を使って行う。
     * 呼び出し側で判定せずに本関数を呼ばないこと。
     */
    @Transaction
    suspend fun grantFirstFreeTrial(reportMonth: String, grantedAt: Long): Boolean {
        // 不正な月IDが保存されることを防ぐ。
        val parsed = runCatching { YearMonth.parse(reportMonth) }.getOrNull()
        require(parsed != null && parsed.toString() == reportMonth) {
            "reportMonth must use YYYY-MM"
        }
        if (hasUsedFreeTrial() || getByMonth(reportMonth) != null) return false

        val id = insertIfAbsent(
            ReportEntitlementEntity(
                reportMonth = reportMonth,
                unlockMethod = ReportEntitlementEntity.FREE_TRIAL,
                grantedAt = grantedAt,
                status = ReportEntitlementEntity.ACTIVE
            )
        )
        return id != -1L
    }
}
