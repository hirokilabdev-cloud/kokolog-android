package com.kokolog.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ReportAnalyzer v1.1 回帰テスト。
 * 30件/30日を基本とし、数量と価値ある気づきの有無を区別する。
 * 全データはメモリ内の架空データ。実際のRoomには書き込まない。
 */
class ReportAnalyzerV11Test {
    private fun analyze(scenario: MonthlyReportQualityFixtures.Scenario): ReportResult =
        ReportAnalyzer.analyze(scenario.logs, emptyList(), scenario.period)

    @Test
    fun standard30RecordsHaveTwoOrThreeIndependentHighlights() {
        val report = analyze(MonthlyReportQualityFixtures.standard30WithPrevious30())
        assertEquals(30, report.summary.totalCount)
        assertEquals(30, report.summary.recordedDays)
        assertTrue(report.readiness.canOfferMonthlyDetail)
        assertEquals(ReportOfferReason.READY, report.readiness.offerReason)
        assertTrue(report.featuredInsights.size in 2..3)
        assertEquals(report.featuredInsights.size, report.readiness.featuredInsightCount)
        assertEquals(report.featuredInsights.first(), report.freePreview)
        // 同じ記録の80%以上を根拠とした、ほぼ同じ発見を重ねない。
        for (a in report.featuredInsights.indices) {
            for (b in a + 1 until report.featuredInsights.size) {
                val left = report.featuredInsights[a].evidenceLogIds.toSet()
                val right = report.featuredInsights[b].evidenceLogIds.toSet()
                if (left.isNotEmpty() && right.isNotEmpty()) {
                    val fraction = left.intersect(right).size.toDouble() / minOf(left.size, right.size)
                    assertTrue("発見の証拠が重複しすぎています: $fraction", fraction < 0.80)
                }
            }
        }
    }

    @Test
    fun realistic20StillOffersWhenIndependentFindingsExist() {
        val report = analyze(MonthlyReportQualityFixtures.realistic20Across15Days())
        assertEquals(20, report.summary.totalCount)
        assertEquals(15, report.summary.recordedDays)
        assertTrue(report.featuredInsights.size >= 2)
        assertTrue(report.readiness.canOfferMonthlyDetail)
    }

    @Test
    fun sixtyRecordsDoNotFloodTheReportWithHighlights() {
        val report = analyze(MonthlyReportQualityFixtures.frequent60Across25Days())
        assertTrue(report.details.insights.size > 3)
        assertEquals(3, report.featuredInsights.size)
        assertTrue(report.readiness.canOfferMonthlyDetail)
    }

    @Test
    fun thirtyUniformRecordsNoLongerEnableAnEmptyPaidReport() {
        val report = analyze(MonthlyReportQualityFixtures.uniform30NoDistinctInsight())
        assertEquals(30, report.summary.totalCount)
        assertEquals(30, report.summary.recordedDays)
        assertTrue(report.readiness.hasSufficientRecords)
        assertTrue(report.details.insights.isEmpty())
        assertTrue(report.featuredInsights.isEmpty())
        assertNull(report.freePreview)
        assertFalse(report.readiness.canOfferMonthlyDetail)
        assertEquals(ReportOfferReason.INSUFFICIENT_FINDINGS, report.readiness.offerReason)
        assertTrue(MonthlyDetailNarrative.overview(report).last().contains("ひとつずつ振り返って"))
    }

    @Test
    fun nineteenRecordsOnOneDayAreNotEligible() {
        val report = analyze(MonthlyReportQualityFixtures.concentrated19OnOneDay())
        assertFalse(report.readiness.hasSufficientRecords)
        assertFalse(report.readiness.canOfferMonthlyDetail)
        assertEquals(ReportOfferReason.INSUFFICIENT_RECORDS, report.readiness.offerReason)
        assertNull(report.freePreview)
    }

    @Test
    fun selectorDiscardsNearDuplicateEvidenceEvenAcrossKinds() {
        val similar = listOf(
            insight(ReportInsightKind.CATEGORY, 600, listOf(1, 2, 3, 4, 5), "仕事の記録に多い感情"),
            insight(ReportInsightKind.TAG, 400, listOf(1, 2, 3, 4, 5), "会議タグに多い感情"),
            insight(ReportInsightKind.INTENSITY, 300, listOf(1, 2, 3, 4), "同じ感情の強度"),
            insight(ReportInsightKind.PERIOD_COMPARISON, 420, listOf(6, 7, 8, 9), "前月からの違い")
        )
        val selected = ReportInsightSelector.select(similar)
        assertEquals(2, selected.size)
        assertEquals(ReportInsightKind.CATEGORY, selected[0].kind)
        assertEquals(ReportInsightKind.PERIOD_COMPARISON, selected[1].kind)
    }

    @Test
    fun selectorCanUseTwoDifferentCategoryDiscoveries() {
        val candidates = listOf(
            insight(ReportInsightKind.CATEGORY, 600, listOf(1, 2, 3), "仕事"),
            insight(ReportInsightKind.CATEGORY, 580, listOf(11, 12, 13), "家族")
        )
        val selected = ReportInsightSelector.select(candidates)
        assertEquals(2, selected.size)
        assertTrue(selected.all { it.kind == ReportInsightKind.CATEGORY })
    }

    @Test
    fun selectorHasNoFakeResultsWhenNothingQualifies() {
        assertTrue(ReportInsightSelector.select(emptyList()).isEmpty())
        assertTrue(ReportInsightSelector.select(listOf(
            insight(ReportInsightKind.CATEGORY, 100, listOf(1), "ひとつ")
        ), maxCount = 0).isEmpty())
    }

    @Test
    fun weeklyReportsRemainFreeOnly() {
        val scenario = MonthlyReportQualityFixtures.standard30WithPrevious30()
        val week = ReportPeriod.weekContaining(scenario.period.startMillis + 14L * 24 * 60 * 60 * 1000)
        val report = ReportAnalyzer.analyze(scenario.logs, emptyList(), week)
        assertFalse(report.readiness.canOfferMonthlyDetail)
        assertEquals(ReportOfferReason.NOT_MONTHLY, report.readiness.offerReason)
    }

    @Test
    fun emptyMonthIsNotAnOffer() {
        val scenario = MonthlyReportQualityFixtures.standard30WithPrevious30()
        val emptyPeriod = ReportPeriod.month(2026, 8)
        val report = ReportAnalyzer.analyze(scenario.logs, emptyList(), emptyPeriod)
        assertEquals(0, report.summary.totalCount)
        assertNull(report.freePreview)
        assertFalse(report.readiness.canOfferMonthlyDetail)
    }

    private fun insight(kind: ReportInsightKind, score: Int, ids: List<Int>, message: String) = ReportInsight(
        kind = kind,
        title = "検証用",
        message = message,
        evidenceLogIds = ids.map(Int::toLong),
        sampleSize = ids.size,
        score = score
    )
}
