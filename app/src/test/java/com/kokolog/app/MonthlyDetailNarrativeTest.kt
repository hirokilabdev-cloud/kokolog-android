package com.kokolog.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** STEP4の説明文が集計値に基づき、少数記録で無理に結論を出さないことを確認する。 */
class MonthlyDetailNarrativeTest {
    private val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
    private fun timestamp(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance(tokyo).apply {
            clear()
            set(year, month - 1, day, hour, 0, 0)
        }.timeInMillis

    private fun log(id: Long, month: Int, day: Int, emotion: String = "ふつう", intensity: Int = 3) =
        EmotionLog(id = id, emotion = emotion, category = "その他", intensity = intensity,
            memo = "", createdAt = timestamp(2026, month, day))

    @Test
    fun emptyMonthHasNoFictitiousInsight() {
        val r = ReportAnalyzer.analyze(emptyList(), emptyList(), ReportPeriod.month(2026, 10, tokyo))
        assertEquals(0, r.summary.totalCount)
        assertEquals(null, MonthlyDetailNarrative.insightFor(r, ReportInsightKind.CATEGORY))
        assertTrue(MonthlyDetailNarrative.overview(r).single().contains("記録がありません"))
        assertTrue(MonthlyDetailNarrative.comparisonText(r).contains("前月に記録がない"))
    }

    @Test
    fun manyRecordsOnOneDayAreNotCalledATrend() {
        val logs = (1L..19L).map { log(it, 6, 18, "ふつう", if (it <= 9L) 4 else 2) }
        val r = ReportAnalyzer.analyze(logs, emptyList(), ReportPeriod.month(2026, 6, tokyo))
        assertEquals(19, r.summary.totalCount)
        assertEquals(1, r.summary.recordedDays)
        assertFalse(r.readiness.hasSufficientRecords)
        assertEquals(2, MonthlyDetailNarrative.overview(r).size)
        assertTrue(MonthlyDetailNarrative.overview(r).last().contains("19件"))
        // 数値自体は正確に表示できるが、感情の原因は推定しない。
        assertTrue(MonthlyDetailNarrative.intensityText(r.details.intensity.first()).contains("19件"))
    }

    @Test
    fun comparisonRefusesToInterpretVerySmallPreviousMonth() {
        val previous = (1L..3L).map { log(it, 9, 2, "ふつう") }
        val current = (4L..21L).map { log(it, 10, 1 + ((it - 4).toInt() % 14), "ふつう") }
        val r = ReportAnalyzer.analyze(previous + current, emptyList(), ReportPeriod.month(2026, 10, tokyo))
        assertTrue(MonthlyDetailNarrative.comparisonText(r).contains("割合の差は判断しません"))
        assertEquals("50.0%", MonthlyDetailNarrative.percent(0.5))
    }
}
