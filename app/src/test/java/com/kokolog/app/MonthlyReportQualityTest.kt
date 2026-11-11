package com.kokolog.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * STEP4 品質確認：30件を基準に、20件・60件・偏りの強いケースも検証する。
 * Roomへ書き込まないローカルJUnitテストなので、実際の記録は変更しない。
 */
class MonthlyReportQualityTest {
    private fun analyze(scenario: MonthlyReportQualityFixtures.Scenario): ReportResult =
        ReportAnalyzer.analyze(scenario.logs, emptyList(), scenario.period)

    @Test
    fun standard30OnePerDayHasMeaningfulFindings() {
        val result = analyze(MonthlyReportQualityFixtures.standard30WithPrevious30())
        assertEquals(30, result.summary.totalCount)
        assertEquals(30, result.summary.recordedDays)
        assertEquals(11, result.summary.categoryCounts.first { it.name == "仕事" }.count)
        assertEquals(11, result.summary.categoryCounts.first { it.name == "家族" }.count)
        assertEquals(6, result.summary.emotionCounts.first { it.name == "ふあん" }.count)
        assertEquals(6, result.details.tags.first { it.name == "会議" }.totalCount)
        assertTrue(result.readiness.canOfferMonthlyDetail)
        assertNotNull("標準の30件でも無料プレビューを生成できること", result.freePreview)
        assertTrue(result.details.insights.any { it.kind == ReportInsightKind.CATEGORY })
        assertTrue(result.details.insights.any { it.kind == ReportInsightKind.TAG })
        assertTrue(MonthlyDetailNarrative.overview(result).isNotEmpty())
    }

    @Test
    fun realistic20Across15DaysStillAllowsUsefulAnalysis() {
        val result = analyze(MonthlyReportQualityFixtures.realistic20Across15Days())
        assertEquals(20, result.summary.totalCount)
        assertEquals(15, result.summary.recordedDays)
        assertTrue(result.readiness.hasSufficientRecords)
        assertTrue(result.readiness.canOfferMonthlyDetail)
        assertNotNull(result.freePreview)
    }

    @Test
    fun sixtyAcrossTwentyFiveDaysDoesNotDoubleCountDates() {
        val result = analyze(MonthlyReportQualityFixtures.frequent60Across25Days())
        assertEquals(60, result.summary.totalCount)
        assertEquals(25, result.summary.recordedDays)
        assertTrue(result.readiness.canOfferMonthlyDetail)
        assertNotNull(result.freePreview)
    }

    @Test
    fun nineteenOnOneDayCannotBeCalledMonthlyTrend() {
        val result = analyze(MonthlyReportQualityFixtures.concentrated19OnOneDay())
        assertEquals(19, result.summary.totalCount)
        assertEquals(1, result.summary.recordedDays)
        assertFalse(result.readiness.canOfferMonthlyDetail)
        assertNull(result.freePreview)
        assertEquals(2, MonthlyDetailNarrative.overview(result).size)
    }

    @Test
    fun previousMonthComparisonUsesRatiosNotOnlyCounts() {
        val result = analyze(MonthlyReportQualityFixtures.standard30WithPrevious30())
        assertEquals(30, result.details.comparison.previousTotalCount)
        assertEquals(30, result.details.comparison.currentTotalCount)
        val anxiety = result.details.comparison.emotionComparisons.first { it.emotion == "ふあん" }
        assertEquals(12, anxiety.previousCount)
        assertEquals(6, anxiety.currentCount)
        assertEquals(-20.0, anxiety.differencePercentagePoints, 0.00001)
    }

    @Test
    fun thirtyUniformRecordsCanHaveNoNotableInsight() {
        val result = analyze(MonthlyReportQualityFixtures.uniform30NoDistinctInsight())
        assertEquals(30, result.summary.totalCount)
        assertEquals(30, result.summary.recordedDays)
        assertNull("均一な記録から無理に発見を作らないこと", result.freePreview)
        assertFalse("発見が0件なら有料レポートを販売しない", result.readiness.canOfferMonthlyDetail)
        assertEquals(ReportOfferReason.INSUFFICIENT_FINDINGS, result.readiness.offerReason)
        assertTrue(result.featuredInsights.isEmpty())
    }
}
