package com.kokolog.app

import java.util.Locale
import kotlin.math.abs

/**
 * STEP4: 月次詳細レポートの文章生成。
 *
 * ReportAnalyzer が計算した数値・裏付けのある Insight だけを文章にする。
 * メモの内容を推測したり、感情の原因や改善・悪化を判定したりしない。
 * UIやAndroidに依存しないため、JVM単体テストで検証できる。
 */
object MonthlyDetailNarrative {

    /** ページ冒頭に置く「今月のココロのまとめ」。 */
    fun overview(report: ReportResult): List<String> {
        val summary = report.summary
        if (summary.totalCount == 0) {
            return listOf("この月には、まだ記録がありません。ココロを残した月を選んでみてください。")
        }
        val lines = mutableListOf<String>()
        lines += "この月は${summary.recordedDays}日間で、${summary.totalCount}件のココロが残されました。"
        val top = summary.emotionCounts.firstOrNull()
        if (top != null) {
            lines += "最も多く記録された感情は「${top.name}」で、${top.count}件でした。"
        }
        // 件数の不足を繰り返し強調しない。事実を伝え、下部の簡略表示へつなぐ。
        if (!report.readiness.hasSufficientRecords) {
            return lines
        }
        // 冒頭の「発見」カードで根拠を紹介するため、総括に同じ文章を重複表示しない。
        if (report.featuredInsights.isEmpty()) {
            lines += "この月に残されたココロを、ひとつずつ振り返ってみましょう。"
        } else {
            lines += "記録から見えてきた特徴を、下の「今月の発見」にまとめました。"
        }
        return lines
    }

    /** 有料レポートに値する根拠がない月も、記録自体は丁寧に扱う。 */
    fun shouldShowDetailedSections(report: ReportResult): Boolean =
        report.readiness.hasSufficientRecords && report.featuredInsights.isNotEmpty()

    /** UIの見出しは実際に見つかった件数に従う。 */
    fun featuredTitle(report: ReportResult): String =
        "今月の発見（${report.featuredInsights.size.coerceAtMost(3)}件）"

    /** グラフの0件行は省き、少ない記録では視覚的な水増しをしない。 */
    fun visibleIntensityLevels(group: ReportEmotionIntensity): List<Pair<Int, Int>> =
        (1..5).map { it to (group.countsByIntensity[it] ?: 0) }.filter { it.second > 0 }

    fun visibleWeekdays(report: ReportResult): List<ReportWeekdayBreakdown> =
        report.details.weekdays.filter { it.totalCount > 0 }

    fun visibleTimeSlots(report: ReportResult): List<ReportTimeBreakdown> =
        report.details.timeSlots.filter { it.totalCount > 0 }

    /** 月に実在しない感情を締めの言葉に混ぜない。 */
    fun closing(report: ReportResult): List<String> {
        val counts = report.summary.emotionCounts
        if (counts.isEmpty()) {
            return listOf("この月にはまだココロが残されていません。いつでも、あなたのペースで記録できます。")
        }
        val emotions = counts.take(2).joinToString("と") { "「${it.name}」" }
        val first = if (counts.size == 1) {
            "今月は${emotions}のココロが記録されていました。"
        } else {
            "今月は${emotions}など、いろいろなココロが残されていました。"
        }
        val second = if (shouldShowDetailedSections(report)) {
            "気になった発見があれば、根拠となった当時の記録も読み返してみてください。"
        } else {
            "気になったココロがあれば、そのときの記録をゆっくり読み返してみてください。"
        }
        return listOf(first, second, "何かを変えなくても、振り返るだけで大丈夫です。")
    }

    /** セクション別に、集計の根拠がある発見のみ取得。 */
    fun insightFor(report: ReportResult, vararg kinds: ReportInsightKind): ReportInsight? =
        report.details.insights.firstOrNull { it.kind in kinds }

    /** 同じ感情内の強さだけ比較する。別感情の強度同士は比べない。 */
    fun intensityText(group: ReportEmotionIntensity): String =
        if (group.validCount < 5) {
            "「${group.emotion}」の強度が記録されたのは${group.validCount}件。今は件数が少ないため、傾向は判断しません。"
        } else {
            "「${group.emotion}」${group.validCount}件のうち、強度4以上は${group.strongCount}件（${percent(group.strongShare)}）でした。"
        }

    /** 前月比較は両月10件以上のときのみ割合差を解説する。 */
    fun comparisonText(report: ReportResult): String {
        val comparison = report.details.comparison
        if (comparison.previousTotalCount == 0) return "前月に記録がないため、感情の割合は比較していません。"
        if (comparison.previousTotalCount < 10 || comparison.currentTotalCount < 10) {
            return "前月${comparison.previousTotalCount}件・今月${comparison.currentTotalCount}件。どちらかの記録が少ないため、感情の割合の差は判断しません。"
        }
        val top = comparison.emotionComparisons.firstOrNull {
            abs(it.differencePercentagePoints) >= 5.0 &&
                it.currentCount + it.previousCount >= 5
        } ?: return "前月と今月を比べましたが、目立つ割合の違いは確認できませんでした。"
        val direction = if (top.differencePercentagePoints >= 0) "高く" else "低く"
        return "「${top.emotion}」の記録割合は前月と比べて${oneDecimal(abs(top.differencePercentagePoints))}ポイント${direction}なっていました。記録頻度の違いも踏まえて振り返ってみましょう。"
    }

    fun percent(value: Double): String = oneDecimal(value * 100.0) + "%"
    fun points(value: Double): String = (if (value > 0) "+" else "") + oneDecimal(value) + "pt"

    private fun oneDecimal(value: Double): String = String.format(Locale.JAPAN, "%.1f", value)
}
