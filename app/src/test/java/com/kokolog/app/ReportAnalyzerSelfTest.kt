package com.kokolog.app

import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.abs

/**
 * ReportAnalyzer v1.0 の回帰テスト。
 * check(...) を使っているので、通常の JVM 上でも実行できる。
 * Android Studio の test 配下に置くこともできる。
 */
object ReportAnalyzerSelfTest {
    private val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
    private val ny = TimeZone.getTimeZone("America/New_York")

    private fun ms(y: Int, m: Int, d: Int, h: Int = 12, min: Int = 0, tz: TimeZone = tokyo): Long =
        GregorianCalendar(tz).apply {
            clear()
            set(y, m - 1, d, h, min, 0)
        }.timeInMillis

    private fun log(
        id: Long, y: Int, m: Int, d: Int,
        emotion: String = "ふあん", category: String = "仕事",
        tags: String = "", intensity: Int = 3, ignored: Boolean = false,
        hour: Int = 12
    ): EmotionLog = EmotionLog(
        id = id, emotion = emotion, category = category, tags = tags,
        isIgnored = ignored, intensity = intensity, memo = "", createdAt = ms(y, m, d, hour)
    )

    private fun assertClose(actual: Double, expected: Double, epsilon: Double = 0.000001) {
        check(abs(actual - expected) < epsilon) { "expected=$expected actual=$actual" }
    }

    fun runAll() {
        periodBoundariesAndMonthEnd()
        weeklyMondayAndDayCounts()
        ignoreAndTagDeduplication()
        intensityAndComparison()
        insightAndReadiness()
        changeEventWithinPeriod()
        daylightSavingPeriod()
        println("ReportAnalyzerSelfTest: 7 checks PASSED")
    }

    private fun periodBoundariesAndMonthEnd() {
        val period = ReportPeriod.month(2026, 10, tokyo)
        val data = listOf(
            log(1, 2026, 9, 30),
            log(2, 2026, 10, 1),
            log(3, 2026, 10, 31).copy(createdAt = ms(2026, 10, 31, 23, 59) + 59999L),
            log(4, 2026, 11, 1)
        )
        val result = ReportAnalyzer.analyze(data, emptyList(), period)
        check(result.summary.totalCount == 2)
        check(result.details.comparison.previousTotalCount == 1)
        check(result.summary.dayCounts.size == 31)
        check(result.summary.dayCounts.first().count == 1)
        check(result.summary.dayCounts.last().count == 1)
    }

    private fun weeklyMondayAndDayCounts() {
        val week = ReportPeriod.weekContaining(ms(2026, 11, 1), tokyo) // 日曜
        check(week.startMillis == ms(2026, 10, 26, 0))
        check(week.endExclusiveMillis == ms(2026, 11, 2, 0))
        val result = ReportAnalyzer.analyze(
            listOf(log(1, 2026, 10, 26), log(2, 2026, 11, 1), log(3, 2026, 11, 2)),
            emptyList(), week
        )
        check(result.summary.totalCount == 2)
        check(result.summary.dayCounts.size == 7)
        check(result.summary.recordedDays == 2)
        check(result.details.weekdays[0].totalCount == 1) // 月曜
        check(result.details.weekdays[6].totalCount == 1) // 日曜
    }

    private fun ignoreAndTagDeduplication() {
        val data = listOf(
            log(1, 2026, 10, 1, tags = "会議, 残業, 会議", ignored = true),
            log(2, 2026, 10, 2, tags = "会議,,", emotion = "うれしい"),
            log(3, 2026, 10, 3, tags = "", emotion = "ふあん")
        )
        val result = ReportAnalyzer.analyze(data, emptyList(), ReportPeriod.month(2026, 10, tokyo))
        check(result.summary.ignoredCount == 1)
        check(result.summary.totalCount == 3)
        check(result.details.tags.first { it.name == "会議" }.totalCount == 2)
        check(result.details.tags.first { it.name == "残業" }.totalCount == 1)
        check(result.details.tags.size == 2)
    }

    private fun intensityAndComparison() {
        val data = listOf(
            log(1, 2026, 10, 1, intensity = 1),
            log(2, 2026, 10, 2, intensity = 4),
            log(3, 2026, 10, 3, intensity = 5),
            log(4, 2026, 10, 4, emotion = "うれしい", intensity = 5),
            log(5, 2026, 9, 1, emotion = "ふあん", intensity = 2),
            log(6, 2026, 9, 2, emotion = "ふあん", intensity = 2)
        )
        val result = ReportAnalyzer.analyze(data, emptyList(), ReportPeriod.month(2026, 10, tokyo))
        val intensity = result.details.intensity.first { it.emotion == "ふあん" }
        check(intensity.validCount == 3 && intensity.strongCount == 2)
        assertClose(intensity.strongShare, 2.0 / 3)
        val diff = result.details.comparison.emotionComparisons.first { it.emotion == "ふあん" }
        assertClose(diff.differencePercentagePoints, -25.0)
    }

    private fun insightAndReadiness() {
        // 20件・10日: 仕事にふあんが集中、家族にうれしいが集中する例。
        val data = (1..10).map { i ->
            log(i.toLong(), 2026, 10, i, emotion = "ふあん", category = "仕事", tags = "会議", intensity = 4)
        } + (1..10).map { i ->
            log((i + 10).toLong(), 2026, 10, i + 10, emotion = "うれしい", category = "家族", intensity = 2)
        }
        val result = ReportAnalyzer.analyze(data, emptyList(), ReportPeriod.month(2026, 10, tokyo))
        check(result.readiness.canOfferMonthlyDetail)
        val preview = requireNotNull(result.freePreview)
        check(preview.kind == ReportInsightKind.CATEGORY)
        check(preview.evidenceLogIds.isNotEmpty())
        val sparse = ReportAnalyzer.analyze(data.take(3), emptyList(), ReportPeriod.month(2026, 10, tokyo))
        check(sparse.freePreview == null)
        check(!sparse.readiness.canOfferMonthlyDetail)
    }

    private fun changeEventWithinPeriod() {
        val main = log(1, 2026, 10, 10)
        val data = listOf(main)
        val changes = listOf(
            EmotionChangeLog(1, 1, "もやもや", 3, "", ms(2026, 10, 11)),
            EmotionChangeLog(2, 1, "あんしん", 2, "", ms(2026, 11, 1)),
            EmotionChangeLog(3, 999, "うれしい", 1, "", ms(2026, 10, 11))
        )
        val result = ReportAnalyzer.analyze(data, changes, ReportPeriod.month(2026, 10, tokyo))
        check(result.details.changes.changeEventCount == 1)
        check(result.details.changes.originalLogsWithChanges == 1)
        check(result.details.changes.transitions.single().fromEmotion == "ふあん")
        check(result.details.changes.transitions.single().toEmotion == "もやもや")
    }

    private fun daylightSavingPeriod() {
        // NYでは2026/3/8にDST移行。0:00起点の日送りがずれないこと。
        val period = ReportPeriod.month(2026, 3, ny)
        val result = ReportAnalyzer.analyze(emptyList(), emptyList(), period)
        check(result.summary.dayCounts.size == 31)
        check(result.summary.dayCounts[7].dayStartMillis == ms(2026, 3, 8, 0, tz = ny))
        check(period.endExclusiveMillis == ms(2026, 4, 1, 0, tz = ny))
    }
}

fun main() = ReportAnalyzerSelfTest.runAll()
