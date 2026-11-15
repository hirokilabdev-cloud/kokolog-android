package com.kokolog.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** STEP4.3-B: 実データは読み書きせず、架空の記録でアルバムとヒントを検証。 */
class MonthlyDetailExperienceTest {
    private val sample = MonthlyReportQualityFixtures.standard30WithPrevious30()

    @Test fun standardMonthCreatesAlbumWithoutEmotionScoring() {
        val albums = MonthlyDetailExperience.albums(sample.logs, sample.period)
        assertEquals(2, albums.size)
        assertEquals("強く残ったココロ", albums.first().title)
        assertTrue(albums.first().evidenceLogIds.isNotEmpty())
        assertTrue(albums.none { "回復" in it.description || "消耗" in it.description || "マイナス" in it.description })
    }

    @Test fun strongAlbumContainsOnlyStrongRecords() {
        val album = MonthlyDetailExperience.albums(sample.logs, sample.period).first()
        val byId = sample.logs.associateBy { it.id }
        assertTrue(album.evidenceLogIds.all { (byId[it]?.intensity ?: 0) in 4..5 })
        assertEquals(album.evidenceLogIds.size, album.totalCount)
        assertEquals(album.evidenceLogIds.distinct(), album.evidenceLogIds)
    }

    @Test fun themeAlbumUsesRecordedTagsOnSeveralDays() {
        val album = MonthlyDetailExperience.albums(sample.logs, sample.period)[1]
        assertTrue(album.title.contains("#"))
        assertTrue(album.evidenceLogIds.size >= 3)
        assertEquals(album.evidenceLogIds.distinct(), album.evidenceLogIds)
    }

    @Test fun excludesPreviousMonthRecords() {
        val current = sample.logs.filter { it.createdAt in sample.period.startMillis until sample.period.endExclusiveMillis }
        assertEquals(MonthlyDetailExperience.albums(current, sample.period),
            MonthlyDetailExperience.albums(sample.logs, sample.period))
    }

    @Test fun zeroRecordsProducesNoAlbumAndNoHint() {
        assertTrue(MonthlyDetailExperience.albums(emptyList(), sample.period).isEmpty())
        assertTrue(MonthlyDetailExperience.hints(emptyList(), sample.period, emptyList(), true).isEmpty())
    }

    @Test fun onlyOneRecordIsNotCalledRepeatedTheme() {
        val one = sample.logs.filter { it.createdAt in sample.period.startMillis until sample.period.endExclusiveMillis }.take(1)
        val albums = MonthlyDetailExperience.albums(one, sample.period)
        assertTrue(albums.none { it.title.contains("繰り返し") })
    }

    @Test fun concentratedDataCannotCreatePersonalHints() {
        val scenario = MonthlyReportQualityFixtures.concentrated19OnOneDay()
        val findings = MonthlyDetailCrossAnalysis.findings(scenario.logs, scenario.period)
        assertTrue(MonthlyDetailExperience.hints(scenario.logs, scenario.period, findings, true).isEmpty())
        assertTrue(MonthlyDetailExperience.albums(scenario.logs, scenario.period).none { it.title.contains("繰り返し") })
    }

    @Test fun uniformMonthDoesNotFabricateThemeOrHint() {
        val uniform = MonthlyReportQualityFixtures.uniform30NoDistinctInsight()
        assertTrue(MonthlyDetailExperience.albums(uniform.logs, uniform.period).none { it.title.contains("繰り返し") })
        val findings = MonthlyDetailCrossAnalysis.findings(uniform.logs, uniform.period)
        assertTrue(MonthlyDetailExperience.hints(uniform.logs, uniform.period, findings, false).isEmpty())
    }

    @Test fun hintsRequireExistingReportOfferGate() {
        val findings = MonthlyDetailCrossAnalysis.findings(sample.logs, sample.period)
        assertTrue(findings.isNotEmpty())
        assertTrue(MonthlyDetailExperience.hints(sample.logs, sample.period, findings, false).isEmpty())
    }

    @Test fun normalMonthYieldsGroundedOptionalHints() {
        val report = ReportAnalyzer.analyze(sample.logs, emptyList(), sample.period)
        val findings = MonthlyDetailCrossAnalysis.findings(sample.logs, sample.period)
        val hints = MonthlyDetailExperience.hints(sample.logs, sample.period, findings,
            report.readiness.canOfferMonthlyDetail)
        assertTrue("30件・30日の標準ケースではヒントが生成される", hints.isNotEmpty())
        assertTrue(hints.size <= 2)
        assertTrue(hints.all { it.evidenceLogIds.size >= 5 && "件" in it.observation })
        assertTrue(hints.all { "みませんか" in it.question })
    }

    @Test fun hintEvidenceMatchesCurrentMonth() {
        val findings = MonthlyDetailCrossAnalysis.findings(sample.logs, sample.period)
        val hints = MonthlyDetailExperience.hints(sample.logs, sample.period, findings, true)
        val currentIds = sample.logs.filter {
            it.createdAt in sample.period.startMillis until sample.period.endExclusiveMillis
        }.map { it.id }.toSet()
        assertTrue(hints.isNotEmpty())
        assertTrue(hints.flatMap { it.evidenceLogIds }.all { it in currentIds })
    }

    @Test fun hintsDoNotClaimCausesOrDiagnosePersonality() {
        val findings = MonthlyDetailCrossAnalysis.findings(sample.logs, sample.period)
        val hints = MonthlyDetailExperience.hints(sample.logs, sample.period, findings, true)
        val text = hints.joinToString(" ") { it.title + it.observation + it.question }
        listOf("原因です", "治る", "回復力", "タイプ診断", "あなたは必ず", "消耗する").forEach {
            assertFalse(it in text)
        }
    }
}
