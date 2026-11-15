package com.kokolog.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * STEP4.3-A: 架空データだけでクロス分析と関連記録の表示内容を確認。
 * 保存済みデータ・Room・課金状態には触れない。
 */
class MonthlyDetailCrossAnalysisTest {
    private val standard = MonthlyReportQualityFixtures.standard30WithPrevious30()

    @Test
    fun thirtyRecordsCanShowCategoryAndTagFacts() {
        val findings = MonthlyDetailCrossAnalysis.findings(standard.logs, standard.period)
        assertEquals(2, findings.size)
        assertTrue(findings.any { it.source == CrossSource.CATEGORY })
        assertTrue(findings.any { it.source == CrossSource.TAG })
        assertTrue(findings.all { it.evidenceLogIds.isNotEmpty() })
        assertTrue(findings.all { it.message.contains("件") })
    }

    @Test
    fun crossFactsReflectActualEmotionAndIntensity() {
        val findings = MonthlyDetailCrossAnalysis.findings(standard.logs, standard.period)
        val meeting = findings.first { it.source == CrossSource.TAG }
        assertEquals("会議", meeting.groupName)
        assertTrue(meeting.message.contains("ふあん"))
        assertTrue(meeting.message.contains("強さ4〜5は6件"))
        assertEquals(6, meeting.evidenceLogIds.size)
        assertFalse(meeting.message.contains("原因"))
    }

    @Test
    fun nineteenRecordsOnOneDayDoNotGenerateCrossTrend() {
        val test = MonthlyReportQualityFixtures.concentrated19OnOneDay()
        assertTrue(MonthlyDetailCrossAnalysis.findings(test.logs, test.period).isEmpty())
    }

    @Test
    fun thirtyUniformRecordsDoNotFabricateInsights() {
        val test = MonthlyReportQualityFixtures.uniform30NoDistinctInsight()
        assertTrue(MonthlyDetailCrossAnalysis.findings(test.logs, test.period).isEmpty())
    }

    @Test
    fun previousMonthIsExcludedFromCrossAnalysis() {
        val all = MonthlyDetailCrossAnalysis.findings(standard.logs, standard.period)
        val current = MonthlyDetailCrossAnalysis.findings(
            standard.logs.filter { it.createdAt in standard.period.startMillis until standard.period.endExclusiveMillis },
            standard.period
        )
        assertEquals(current, all)
    }

    @Test
    fun maxCountIsRespected() {
        assertTrue(MonthlyDetailCrossAnalysis.findings(standard.logs, standard.period, 0).isEmpty())
        assertEquals(1, MonthlyDetailCrossAnalysis.findings(standard.logs, standard.period, 1).size)
    }

    @Test
    fun duplicateAndEmptyTagsAreRemoved() {
        assertEquals(listOf("会議", "家族"), MonthlyDetailCrossAnalysis.parseTags("#会議, 会議, , #家族,家族"))
        assertEquals("#会議  #家族", MonthlyDetailRecordPreview.tags("#会議, 会議,#家族"))
    }

    @Test
    fun lengthyTagsAreNotSilentlyDiscarded() {
        assertEquals("#a  #b  #c  #d  ほか1タグ", MonthlyDetailRecordPreview.tags("a,b,c,d,e"))
    }

    @Test
    fun memoPreviewKeepsOriginalWordsWithoutInterpretation() {
        assertEquals(null, MonthlyDetailRecordPreview.memo("   \n"))
        assertEquals("今日は 少しうれしい", MonthlyDetailRecordPreview.memo("今日は   少しうれしい"))
        assertEquals("abcd…", MonthlyDetailRecordPreview.memo("abcdefghijk", 5))
    }

    @Test
    fun intensityIsDisplayedOnlyForValidValues() {
        assertEquals("強さ 5/5", MonthlyDetailRecordPreview.intensity(5))
        assertEquals(null, MonthlyDetailRecordPreview.intensity(0))
        assertEquals(null, MonthlyDetailRecordPreview.intensity(6))
    }
}
