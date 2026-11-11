package com.kokolog.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * STEP4.1: 画面に表示する情報量と文章の品質を検証する。
 * すべて架空データをメモリで集計し、Roomや実際のココログには触れない。
 */
class MonthlyDetailPresentationTest {
    private fun report(scenario: MonthlyReportQualityFixtures.Scenario) =
        ReportAnalyzer.analyze(scenario.logs, emptyList(), scenario.period)

    @Test
    fun normal30DayMonthShowsHighlightsAndDetailedSections() {
        val r = report(MonthlyReportQualityFixtures.standard30WithPrevious30())
        assertTrue(MonthlyDetailNarrative.shouldShowDetailedSections(r))
        assertTrue(r.featuredInsights.size in 2..3)
        assertEquals("今月の発見（${r.featuredInsights.size}件）", MonthlyDetailNarrative.featuredTitle(r))
        // 発見の文章はハイライト側に出す。概要で同じ話を繰り返さない。
        assertTrue(MonthlyDetailNarrative.overview(r).last().contains("下の「今月の発見」"))
        assertFalse(MonthlyDetailNarrative.overview(r).any { it == r.featuredInsights.first().message })
    }

    @Test
    fun nineteenOnOneDayUsesCompactMode() {
        val r = report(MonthlyReportQualityFixtures.concentrated19OnOneDay())
        assertEquals(19, r.summary.totalCount)
        assertEquals(1, r.summary.recordedDays)
        assertFalse(MonthlyDetailNarrative.shouldShowDetailedSections(r))
        assertEquals(1, MonthlyDetailNarrative.visibleWeekdays(r).size)
        assertEquals(1, MonthlyDetailNarrative.visibleTimeSlots(r).size)
    }

    @Test
    fun uniform30DoesNotFillScreenWithUnsupportedConclusions() {
        val r = report(MonthlyReportQualityFixtures.uniform30NoDistinctInsight())
        assertTrue(r.readiness.hasSufficientRecords)
        assertFalse(r.readiness.canOfferMonthlyDetail)
        assertFalse(MonthlyDetailNarrative.shouldShowDetailedSections(r))
        assertTrue(r.featuredInsights.isEmpty())
        assertTrue(MonthlyDetailNarrative.overview(r).last().contains("ひとつずつ振り返って"))
    }

    @Test
    fun graphOnlyIncludesRecordedIntensityLevels() {
        val r = report(MonthlyReportQualityFixtures.concentrated19OnOneDay())
        val top = r.details.intensity.first()
        val present = MonthlyDetailNarrative.visibleIntensityLevels(top)
        assertTrue(present.isNotEmpty())
        assertTrue(present.all { it.second > 0 })
        assertEquals(top.validCount, present.sumOf { it.second })
    }

    @Test
    fun closingUsesOnlyActualRecordedEmotions() {
        val r = report(MonthlyReportQualityFixtures.uniform30NoDistinctInsight())
        val closing = MonthlyDetailNarrative.closing(r).joinToString(" ")
        assertTrue(r.summary.emotionCounts.isNotEmpty())
        assertTrue(closing.contains(r.summary.emotionCounts.first().name))
        assertFalse(closing.contains("うれしいことも、不安だったことも"))
    }

    @Test
    fun emptyMonthDoesNotShowDetailedSectionsOrInventEmotions() {
        val base = MonthlyReportQualityFixtures.standard30WithPrevious30()
        val r = ReportAnalyzer.analyze(base.logs, emptyList(), ReportPeriod.month(2026, 8))
        assertEquals(0, r.summary.totalCount)
        assertFalse(MonthlyDetailNarrative.shouldShowDetailedSections(r))
        assertTrue(MonthlyDetailNarrative.closing(r).single().contains("残されていません"))
        assertTrue(MonthlyDetailNarrative.visibleWeekdays(r).isEmpty())
    }
}
