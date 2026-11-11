package com.kokolog.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 架空の記録だけで、販売案内と内部テスト解放を独立に検証する。 */
class MonthlyDetailAccessPolicyTest {
    private fun report(scenario: MonthlyReportQualityFixtures.Scenario) =
        ReportAnalyzer.analyze(scenario.logs, emptyList(), scenario.period)

    @Test fun regularReleaseIsAlwaysLocked() {
        assertFalse(MonthlyDetailAccessPolicy.isTesterPreviewBuild("release", false))
        assertFalse(MonthlyDetailAccessPolicy.isTesterPreviewBuild("release", true))
    }

    @Test fun internalPreviewIsUnlockedEvenThoughNotDebuggable() {
        assertTrue(MonthlyDetailAccessPolicy.isTesterPreviewBuild("internalPreview", false))
        assertTrue(MonthlyDetailAccessPolicy.isTesterPreviewBuild("debug", true))
        assertFalse(MonthlyDetailAccessPolicy.isTesterPreviewBuild("debug", false))
        assertFalse(MonthlyDetailAccessPolicy.isTesterPreviewBuild("staging", false))
    }

    @Test fun thirtyDailyRecordsCanShowUpcomingPaidDetail() {
        val ui = MonthlyDetailAccessPolicy.presentation(
            report(MonthlyReportQualityFixtures.standard30WithPrevious30()), false
        )
        assertTrue(ui.showPaidIntroduction)
        assertTrue(ui.isEligibleForPaidDetail)
        assertFalse(ui.showTesterPreview)
    }

    @Test fun concentratedNineteenRecordsNeverShowsPurchaseIntroduction() {
        val ui = MonthlyDetailAccessPolicy.presentation(
            report(MonthlyReportQualityFixtures.concentrated19OnOneDay()), false
        )
        assertFalse(ui.showPaidIntroduction)
        assertFalse(ui.isEligibleForPaidDetail)
    }

    @Test fun uniformThirtyRecordsNeverShowsPurchaseIntroduction() {
        val ui = MonthlyDetailAccessPolicy.presentation(
            report(MonthlyReportQualityFixtures.uniform30NoDistinctInsight()), false
        )
        assertFalse(ui.showPaidIntroduction)
        assertFalse(ui.isEligibleForPaidDetail)
    }

    @Test fun testersCanInspectInsufficientMonthsWithoutOfferingToPurchase() {
        val ui = MonthlyDetailAccessPolicy.presentation(
            report(MonthlyReportQualityFixtures.concentrated19OnOneDay()), true
        )
        assertFalse(ui.showPaidIntroduction)
        assertTrue(ui.showTesterPreview)
        assertFalse(ui.isEligibleForPaidDetail)
    }

    @Test fun emptyMonthHasNoPaidIntroductionButCanBeQaPreviewed() {
        val base = MonthlyReportQualityFixtures.standard30WithPrevious30()
        val empty = ReportAnalyzer.analyze(base.logs, emptyList(), ReportPeriod.month(2026, 8))
        val ui = MonthlyDetailAccessPolicy.presentation(empty, true)
        assertFalse(ui.showPaidIntroduction)
        assertTrue(ui.showTesterPreview)
    }
}
