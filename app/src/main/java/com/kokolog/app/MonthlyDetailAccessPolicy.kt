package com.kokolog.app

/**
 * STEP4.2: 月次詳細レポートの案内と内部テスト解放を分離する純粋な判定ロジック。
 *
 * 内部テスト用の解放は「購入・初回無料利用権」を意味しない。
 * 正式販売時は Billing の購入資格・復元確認を別途実装すること。
 */
object MonthlyDetailAccessPolicy {

    /**
     * デバッグ実行、および内部テスト向けの署名付きAABだけプレビューを許可する。
     * 「release」では、誤って端末設定を変えても解放しない。
     */
    fun isTesterPreviewBuild(buildType: String, isDebuggable: Boolean): Boolean =
        (buildType == "debug" && isDebuggable) || buildType == "internalPreview"

    /**
     * 購入案内は「月次詳細を提供できる」と確認された月だけ。
     * 内部テスト用プレビューは分析不足月も画面検証できるよう独立させる。
     */
    fun presentation(
        report: ReportResult,
        testerPreviewEnabled: Boolean
    ): MonthlyDetailOfferPresentation = MonthlyDetailOfferPresentation(
        showPaidIntroduction = report.readiness.canOfferMonthlyDetail &&
            report.summary.totalCount > 0,
        showTesterPreview = testerPreviewEnabled,
        isEligibleForPaidDetail = report.readiness.canOfferMonthlyDetail
    )
}

data class MonthlyDetailOfferPresentation(
    val showPaidIntroduction: Boolean,
    val showTesterPreview: Boolean,
    val isEligibleForPaidDetail: Boolean
)
