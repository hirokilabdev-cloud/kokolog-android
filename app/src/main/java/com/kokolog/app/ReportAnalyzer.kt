package com.kokolog.app

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * KokoLog Report Analyzer v1.0
 *
 * ・Room の Entity/DAO/DB スキーマは変更しない。
 * ・このファイルは Android UI に依存しない、純粋な集計処理。
 * ・集計対象は「記録日時 (createdAt)」で決める。編集された日時も反映される。
 * ・期間は [開始日時, 終了日時) で判定し、月末・週末の境界を安全に扱う。
 * ・「気にしない」は一覧で非表示でも集計に含め、件数を別に表示する。
 * ・感情の良し悪しを判断したり、出来事の原因を断定したりしない。
 */

enum class ReportType { WEEK, MONTH }

/** 日時の表示・集計に用いる単位。端末のタイムゾーンに従う。 */
data class ReportPeriod(
    val type: ReportType,
    val startMillis: Long,
    val endExclusiveMillis: Long,
    val previousStartMillis: Long,
    val previousEndExclusiveMillis: Long,
    val timeZoneId: String
) {
    init {
        require(startMillis < endExclusiveMillis) { "対象期間が不正です" }
        require(previousStartMillis < previousEndExclusiveMillis) { "比較期間が不正です" }
        require(previousEndExclusiveMillis == startMillis) { "比較期間は対象期間の直前である必要があります" }
    }

    companion object {
        /** 指定された年月の1日0:00〜翌月1日0:00を対象にする。month は 1〜12。 */
        fun month(
            year: Int,
            month: Int,
            timeZone: TimeZone = TimeZone.getDefault()
        ): ReportPeriod {
            require(year in 1..9999 && month in 1..12)
            val start = calendarAtMidnight(year, month, 1, timeZone)
            val end = start.clone() as Calendar
            end.add(Calendar.MONTH, 1)
            val previous = start.clone() as Calendar
            previous.add(Calendar.MONTH, -1)
            return ReportPeriod(
                type = ReportType.MONTH,
                startMillis = start.timeInMillis,
                endExclusiveMillis = end.timeInMillis,
                previousStartMillis = previous.timeInMillis,
                previousEndExclusiveMillis = start.timeInMillis,
                timeZoneId = timeZone.id
            )
        }

        /** 指定した日時を含む月曜0:00〜翌週月曜0:00を対象にする。 */
        fun weekContaining(
            timestampMillis: Long,
            timeZone: TimeZone = TimeZone.getDefault()
        ): ReportPeriod {
            val start = GregorianCalendar(timeZone).apply {
                timeInMillis = timestampMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // Calendar.SUNDAY=1, MONDAY=2。月曜日から何日経ったか。
                val daysFromMonday = (get(Calendar.DAY_OF_WEEK) + 5) % 7
                add(Calendar.DAY_OF_MONTH, -daysFromMonday)
            }
            val end = start.clone() as Calendar
            end.add(Calendar.DAY_OF_MONTH, 7)
            val previous = start.clone() as Calendar
            previous.add(Calendar.DAY_OF_MONTH, -7)
            return ReportPeriod(
                type = ReportType.WEEK,
                startMillis = start.timeInMillis,
                endExclusiveMillis = end.timeInMillis,
                previousStartMillis = previous.timeInMillis,
                previousEndExclusiveMillis = start.timeInMillis,
                timeZoneId = timeZone.id
            )
        }

        private fun calendarAtMidnight(
            year: Int,
            month: Int,
            day: Int,
            timeZone: TimeZone
        ): Calendar = GregorianCalendar(timeZone).apply {
            clear()
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}

/** 割合は 0.0〜1.0 の値で保持。パーセント表記はUI側で行う。 */
data class ReportCount(val name: String, val count: Int, val share: Double)
data class ReportDayCount(val dayStartMillis: Long, val count: Int)

data class ReportSummary(
    val totalCount: Int,
    val recordedDays: Int,
    val ignoredCount: Int,
    val emotionCounts: List<ReportCount>,
    val categoryCounts: List<ReportCount>,
    val dayCounts: List<ReportDayCount>
)

/** 同じ感情の中だけで強度を比較する。強度不正値は別件数で保持。 */
data class ReportEmotionIntensity(
    val emotion: String,
    val validCount: Int,
    val countsByIntensity: Map<Int, Int>,
    val strongCount: Int, // 強度 4・5 の件数
    val strongShare: Double
)

enum class ReportTimeSlot(val label: String) {
    EARLY_MORNING("深夜"), // 23:00〜翌4:59
    MORNING("朝"),         // 5:00〜10:59
    AFTERNOON("昼"),       // 11:00〜16:59
    EVENING("夕方・夜")    // 17:00〜22:59
}

/** 曜日1=月曜、7=日曜。各感情の割合はその曜日の記録件数を分母にする。 */
data class ReportWeekdayBreakdown(
    val dayOfWeek: Int,
    val totalCount: Int,
    val emotions: List<ReportCount>
)

data class ReportTimeBreakdown(
    val slot: ReportTimeSlot,
    val totalCount: Int,
    val emotions: List<ReportCount>
)

/** カテゴリー/タグ内における感情分布。タグは1件につき複数含まれる場合がある。 */
data class ReportGroupBreakdown(
    val name: String,
    val totalCount: Int,
    val emotions: List<ReportCount>
)

data class ReportEmotionComparison(
    val emotion: String,
    val previousCount: Int,
    val currentCount: Int,
    val previousShare: Double,
    val currentShare: Double,
    val differencePercentagePoints: Double
)

data class ReportComparison(
    val previousTotalCount: Int,
    val currentTotalCount: Int,
    val totalDifference: Int,
    val previousRecordedDays: Int,
    val currentRecordedDays: Int,
    val emotionComparisons: List<ReportEmotionComparison>
)

/** 「その後のココロ」は元の記録に紐づけて扱う。因果関係や改善/悪化は判定しない。 */
data class ReportEmotionTransition(
    val fromEmotion: String,
    val toEmotion: String,
    val count: Int
)

data class ReportChangeSummary(
    val originalLogsWithChanges: Int,
    val changeEventCount: Int,
    val transitions: List<ReportEmotionTransition>
)

enum class ReportInsightKind { CATEGORY, TAG, TIME_SLOT, WEEKDAY, INTENSITY, PERIOD_COMPARISON }

/** 根拠となる記録IDを保持し、将来「この記録を読み返す」へつなぐ。 */
data class ReportInsight(
    val kind: ReportInsightKind,
    val title: String,
    val message: String,
    val evidenceLogIds: List<Long>,
    val sampleSize: Int,
    val score: Int
)

/** 詳細分析を販売/無料解放する前に、提供可能な内容を判定する。 */
data class ReportReadiness(
    val hasSufficientRecords: Boolean,
    val availableSections: List<String>,
    val canOfferMonthlyDetail: Boolean
)

data class ReportDetails(
    val intensity: List<ReportEmotionIntensity>,
    val weekdays: List<ReportWeekdayBreakdown>,
    val timeSlots: List<ReportTimeBreakdown>,
    val categories: List<ReportGroupBreakdown>,
    val tags: List<ReportGroupBreakdown>,
    val comparison: ReportComparison,
    val changes: ReportChangeSummary,
    val insights: List<ReportInsight>
)

data class ReportResult(
    val period: ReportPeriod,
    val summary: ReportSummary,
    val details: ReportDetails,
    val freePreview: ReportInsight?,
    val readiness: ReportReadiness
)

/**
 * 公開API: 既存のDAOから取得した全件を渡して集計する。
 *
 * 例:
 *   val result = ReportAnalyzer.analyze(
 *       logs = emotionLogDao.getAllLogs(),
 *       changeLogs = emotionChangeLogDao.getAllChangeLogs(),
 *       period = ReportPeriod.month(2026, 10)
 *   )
 *
 * 注意: Reflection画面の「表示中だけのlogs」ではなく、Roomの全件を渡すこと。
 */
object ReportAnalyzer {
    fun analyze(
        logs: List<EmotionLog>,
        changeLogs: List<EmotionChangeLog>,
        period: ReportPeriod
    ): ReportResult {
        val timeZone = TimeZone.getTimeZone(period.timeZoneId)
        val current = logs.filter {
            it.createdAt >= period.startMillis && it.createdAt < period.endExclusiveMillis
        }
        val previous = logs.filter {
            it.createdAt >= period.previousStartMillis && it.createdAt < period.previousEndExclusiveMillis
        }

        val recordedDays = current.map { dayStart(it.createdAt, timeZone) }.toSet().size
        val dayCountsByStart = current.groupingBy { dayStart(it.createdAt, timeZone) }.eachCount()
        val calendarDays = mutableListOf<ReportDayCount>()
        val cursor = GregorianCalendar(timeZone).apply { timeInMillis = period.startMillis }
        while (cursor.timeInMillis < period.endExclusiveMillis) {
            calendarDays += ReportDayCount(
                dayStartMillis = cursor.timeInMillis,
                count = dayCountsByStart[cursor.timeInMillis] ?: 0
            )
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }

        val summary = ReportSummary(
            totalCount = current.size,
            recordedDays = recordedDays,
            ignoredCount = current.count { it.isIgnored },
            emotionCounts = counts(current) { it.emotion },
            categoryCounts = counts(current) { it.category },
            dayCounts = calendarDays
        )

        val intensities = current.filter { it.intensity in 1..5 }
            .groupBy { it.emotion }
            .map { (emotion, items) ->
                val levels = (1..5).associateWith { level -> items.count { it.intensity == level } }
                val strong = (levels[4] ?: 0) + (levels[5] ?: 0)
                ReportEmotionIntensity(
                    emotion = emotion,
                    validCount = items.size,
                    countsByIntensity = levels,
                    strongCount = strong,
                    strongShare = ratio(strong, items.size)
                )
            }
            .sortedWith(compareByDescending<ReportEmotionIntensity> { it.validCount }.thenBy { it.emotion })

        val weekdays = (1..7).map { day ->
            val dayLogs = current.filter { isoDayOfWeek(it.createdAt, timeZone) == day }
            ReportWeekdayBreakdown(day, dayLogs.size, counts(dayLogs) { it.emotion })
        }
        val timeSlots = ReportTimeSlot.values().map { slot ->
            val slotLogs = current.filter { slotOf(it.createdAt, timeZone) == slot }
            ReportTimeBreakdown(slot, slotLogs.size, counts(slotLogs) { it.emotion })
        }
        val categories = current.groupBy { it.category }
            .map { (name, items) -> ReportGroupBreakdown(name, items.size, counts(items) { it.emotion }) }
            .sortedWith(compareByDescending<ReportGroupBreakdown> { it.totalCount }.thenBy { it.name })

        // CSV形式のタグ。前後の空白・重複・空文字を除外する。
        val tagsByLog = current.associate { it.id to splitTags(it.tags) }
        val tagNames = tagsByLog.values.flatten().toSet()
        val tags = tagNames.map { tag ->
            val items = current.filter { tag in (tagsByLog[it.id] ?: emptySet()) }
            ReportGroupBreakdown(tag, items.size, counts(items) { it.emotion })
        }.sortedWith(compareByDescending<ReportGroupBreakdown> { it.totalCount }.thenBy { it.name })

        val previousEmotionCounts = previous.groupingBy { it.emotion }.eachCount()
        val currentEmotionCounts = current.groupingBy { it.emotion }.eachCount()
        val comparison = ReportComparison(
            previousTotalCount = previous.size,
            currentTotalCount = current.size,
            totalDifference = current.size - previous.size,
            previousRecordedDays = previous.map { dayStart(it.createdAt, timeZone) }.toSet().size,
            currentRecordedDays = recordedDays,
            emotionComparisons = (previousEmotionCounts.keys + currentEmotionCounts.keys)
                .map { emotion ->
                    val prevCount = previousEmotionCounts[emotion] ?: 0
                    val nowCount = currentEmotionCounts[emotion] ?: 0
                    val prevShare = ratio(prevCount, previous.size)
                    val nowShare = ratio(nowCount, current.size)
                    ReportEmotionComparison(
                        emotion, prevCount, nowCount, prevShare, nowShare,
                        (nowShare - prevShare) * 100.0
                    )
                }
                .sortedWith(compareByDescending<ReportEmotionComparison> { abs(it.differencePercentagePoints) }.thenBy { it.emotion })
        )

        // 元のログが対象期間内にある変化履歴のみ集計。
        // 月をまたいだ変化（翌月に記録された追記）は v1.0 では当月レポートの対象外。
        val byId = current.filter { it.id > 0 }.associateBy { it.id }
        val validChanges = changeLogs.filter { change ->
            val original = byId[change.originalLogId]
            original != null && change.createdAt >= original.createdAt &&
                change.createdAt < period.endExclusiveMillis
        }
        val changesByOriginal = validChanges.groupBy { it.originalLogId }
        val transitionCounts = mutableMapOf<Pair<String, String>, Int>()
        changesByOriginal.forEach { (id, events) ->
            var lastEmotion = byId.getValue(id).emotion
            events.sortedWith(compareBy<EmotionChangeLog> { it.createdAt }.thenBy { it.id })
                .forEach { event ->
                    val key = lastEmotion to event.emotion
                    transitionCounts[key] = (transitionCounts[key] ?: 0) + 1
                    lastEmotion = event.emotion
                }
        }
        val changeSummary = ReportChangeSummary(
            originalLogsWithChanges = changesByOriginal.size,
            changeEventCount = validChanges.size,
            transitions = transitionCounts.map { (pair, n) ->
                ReportEmotionTransition(pair.first, pair.second, n)
            }.sortedWith(compareByDescending<ReportEmotionTransition> { it.count }
                .thenBy { it.fromEmotion }.thenBy { it.toEmotion })
        )

        val insights = createInsights(
            current, previous, summary, intensities, weekdays, timeSlots,
            categories, tags, comparison, timeZone, period
        )
        val readiness = evaluateReadiness(
            period, summary, intensities, categories, tags, previous.size
        )
        val details = ReportDetails(
            intensity = intensities,
            weekdays = weekdays,
            timeSlots = timeSlots,
            categories = categories,
            tags = tags,
            comparison = comparison,
            changes = changeSummary,
            insights = insights
        )
        return ReportResult(period, summary, details, insights.firstOrNull(), readiness)
    }

    private fun evaluateReadiness(
        period: ReportPeriod,
        summary: ReportSummary,
        intensities: List<ReportEmotionIntensity>,
        categories: List<ReportGroupBreakdown>,
        tags: List<ReportGroupBreakdown>,
        previousCount: Int
    ): ReportReadiness {
        val sufficient = summary.totalCount >= 20 && summary.recordedDays >= 7
        val sections = mutableListOf<String>()
        if (intensities.any { it.validCount >= 5 }) sections += "感情強度"
        if (sufficient) sections += "曜日・時間帯"
        if (categories.any { it.totalCount >= 5 } || tags.any { it.totalCount >= 5 }) {
            sections += "カテゴリー・タグ"
        }
        if (summary.totalCount >= 10 && previousCount >= 10) sections += "前期間比較"
        return ReportReadiness(
            hasSufficientRecords = sufficient,
            availableSections = sections,
            // 有料で出せるだけの材料があるときだけ案内する。
            canOfferMonthlyDetail = period.type == ReportType.MONTH && sufficient && sections.size >= 2
        )
    }

    /**
     * 「小さな発見」を生成。例示用の文章ではなく、必ず実際の集計結果から生成する。
     * 数が少ない場合には無理に発見を作らず、空のリストを返す。
     */
    private fun createInsights(
        current: List<EmotionLog>,
        previous: List<EmotionLog>,
        summary: ReportSummary,
        intensities: List<ReportEmotionIntensity>,
        weekdays: List<ReportWeekdayBreakdown>,
        timeSlots: List<ReportTimeBreakdown>,
        categories: List<ReportGroupBreakdown>,
        tags: List<ReportGroupBreakdown>,
        comparison: ReportComparison,
        timeZone: TimeZone,
        period: ReportPeriod
    ): List<ReportInsight> {
        // 週次は少量データが多いため、定量的な発見を無理に生成しない。
        if (summary.totalCount < 10 || summary.recordedDays < 5) return emptyList()
        val result = mutableListOf<ReportInsight>()

        // カテゴリーごとの感情の割合を「それ以外の記録」と比較する。
        for (group in categories) {
            if (group.totalCount < 5 || current.size - group.totalCount < 10) continue
            val included = current.filter { it.category == group.name }
            val excluded = current.filter { it.category != group.name }
            for (entry in group.emotions) {
                if (entry.count < 3) continue
                val otherShare = ratio(excluded.count { it.emotion == entry.name }, excluded.size)
                val difference = entry.share - otherShare
                if (difference < 0.20) continue
                result += ReportInsight(
                    kind = ReportInsightKind.CATEGORY,
                    title = "カテゴリーと感情のつながり",
                    message = "「${group.name}」の記録では「${entry.name}」が${entry.count}/${group.totalCount}件で、ほかのカテゴリーより割合が高く記録されていました。",
                    evidenceLogIds = included.filter { it.emotion == entry.name }.map { it.id },
                    sampleSize = group.totalCount,
                    score = 500 + strengthScore(difference, group.totalCount)
                )
            }
        }

        // 同じ記録に複数のタグがある場合も、タグごとには1件と数える。
        for (group in tags) {
            if (group.totalCount < 5 || current.size - group.totalCount < 10) continue
            val included = current.filter { group.name in splitTags(it.tags) }
            val excluded = current.filterNot { group.name in splitTags(it.tags) }
            for (entry in group.emotions) {
                if (entry.count < 3) continue
                val otherShare = ratio(excluded.count { it.emotion == entry.name }, excluded.size)
                val difference = entry.share - otherShare
                if (difference < 0.20) continue
                result += ReportInsight(
                    kind = ReportInsightKind.TAG,
                    title = "タグと感情のつながり",
                    message = "「${group.name}」タグの記録${group.totalCount}件のうち、「${entry.name}」は${entry.count}件でした。タグのない記録などと比べても割合が高くなっています。",
                    evidenceLogIds = included.filter { it.emotion == entry.name }.map { it.id },
                    sampleSize = group.totalCount,
                    score = 300 + strengthScore(difference, group.totalCount)
                )
            }
        }

        // 時間帯は同じ時間帯の全記録を分母にして他の時間帯と比べる。
        if (summary.totalCount >= 20 && summary.recordedDays >= 7) {
            for (group in timeSlots) {
                if (group.totalCount < 5 || current.size - group.totalCount < 10) continue
                val included = current.filter { slotOf(it.createdAt, timeZone) == group.slot }
                val excluded = current.filterNot { slotOf(it.createdAt, timeZone) == group.slot }
                for (entry in group.emotions) {
                    if (entry.count < 3) continue
                    val difference = entry.share - ratio(excluded.count { it.emotion == entry.name }, excluded.size)
                    if (difference < 0.25) continue
                    result += ReportInsight(
                        kind = ReportInsightKind.TIME_SLOT,
                        title = "時間帯ごとのココロ",
                        message = "「${group.slot.label}」の記録${group.totalCount}件では「${entry.name}」が${entry.count}件で、ほかの時間帯より割合が高く記録されました。",
                        evidenceLogIds = included.filter { it.emotion == entry.name }.map { it.id },
                        sampleSize = group.totalCount,
                        score = 350 + strengthScore(difference, group.totalCount)
                    )
                }
            }
        }

        // 曜日の傾向は月次限定。1週間の曜日データだけでは繰り返しを判断できない。
        if (period.type == ReportType.MONTH && summary.totalCount >= 20 && summary.recordedDays >= 7) {
            val dayNames = listOf("月", "火", "水", "木", "金", "土", "日")
            for (group in weekdays) {
                if (group.totalCount < 5 || current.size - group.totalCount < 10) continue
                val included = current.filter { isoDayOfWeek(it.createdAt, timeZone) == group.dayOfWeek }
                // 同じ曜日に複数の日付で記録していることを確認する。
                if (included.map { dayStart(it.createdAt, timeZone) }.toSet().size < 2) continue
                val excluded = current.filterNot { isoDayOfWeek(it.createdAt, timeZone) == group.dayOfWeek }
                for (entry in group.emotions) {
                    if (entry.count < 3) continue
                    val difference = entry.share - ratio(excluded.count { it.emotion == entry.name }, excluded.size)
                    if (difference < 0.25) continue
                    result += ReportInsight(
                        kind = ReportInsightKind.WEEKDAY,
                        title = "曜日ごとのココロ",
                        message = "${dayNames[group.dayOfWeek - 1]}曜日の記録では「${entry.name}」が${entry.count}/${group.totalCount}件で、ほかの曜日より割合が高く記録されました。",
                        evidenceLogIds = included.filter { it.emotion == entry.name }.map { it.id },
                        sampleSize = group.totalCount,
                        score = 350 + strengthScore(difference, group.totalCount)
                    )
                }
            }
        }

        // 感情ごとの強度4・5の割合。感情の種類をまたいだ平均化はしない。
        for (group in intensities) {
            if (group.validCount < 5 || group.strongCount < 3 || group.strongShare < 0.60) continue
            result += ReportInsight(
                kind = ReportInsightKind.INTENSITY,
                title = "強く残ったココロ",
                message = "「${group.emotion}」として記録された${group.validCount}件のうち、${group.strongCount}件が強度4以上でした。",
                evidenceLogIds = current.filter { it.emotion == group.emotion && it.intensity >= 4 }.map { it.id },
                sampleSize = group.validCount,
                score = 200 + strengthScore(group.strongShare, group.validCount)
            )
        }

        // 前月/前週で各感情の「割合」の差を比較。件数増減と混同しない。
        if (previous.size >= 10 && current.size >= 10) {
            for (entry in comparison.emotionComparisons) {
                val delta = entry.differencePercentagePoints
                if (abs(delta) < 15.0) continue
                if (entry.currentCount + entry.previousCount < 5) continue
                val direction = if (delta > 0) "高く" else "低く"
                result += ReportInsight(
                    kind = ReportInsightKind.PERIOD_COMPARISON,
                    title = "前の期間との違い",
                    message = "「${entry.emotion}」の記録割合は、前の期間と比べて${abs(delta).roundToInt()}ポイント${direction}なっていました。",
                    evidenceLogIds = current.filter { it.emotion == entry.emotion }.map { it.id },
                    sampleSize = current.size + previous.size,
                    score = 400 + strengthScore(abs(delta) / 100.0, minOf(current.size, previous.size))
                )
            }
        }

        // スコアは優先順位付けの内部目安であり、分析の統計的信頼度ではない。
        return result.sortedWith(compareByDescending<ReportInsight> { it.score }
            .thenByDescending { it.sampleSize }
            .thenBy { it.kind.name }
            .thenBy { it.message })
    }

    private fun strengthScore(difference: Double, sampleSize: Int): Int =
        (difference * 100).roundToInt() + minOf(sampleSize, 20)

    private fun ratio(count: Int, denominator: Int): Double =
        if (denominator == 0) 0.0 else count.toDouble() / denominator

    private fun counts(logs: List<EmotionLog>, key: (EmotionLog) -> String): List<ReportCount> =
        logs.groupingBy(key).eachCount()
            .map { (name, count) -> ReportCount(name, count, ratio(count, logs.size)) }
            .sortedWith(compareByDescending<ReportCount> { it.count }.thenBy { it.name })

    private fun splitTags(tags: String): Set<String> =
        tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private fun dayStart(timestamp: Long, timeZone: TimeZone): Long =
        GregorianCalendar(timeZone).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun isoDayOfWeek(timestamp: Long, timeZone: TimeZone): Int =
        GregorianCalendar(timeZone).apply { timeInMillis = timestamp }
            .get(Calendar.DAY_OF_WEEK).let { (it + 5) % 7 + 1 }

    private fun slotOf(timestamp: Long, timeZone: TimeZone): ReportTimeSlot {
        val hour = GregorianCalendar(timeZone).apply { timeInMillis = timestamp }
            .get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> ReportTimeSlot.MORNING
            in 11..16 -> ReportTimeSlot.AFTERNOON
            in 17..22 -> ReportTimeSlot.EVENING
            else -> ReportTimeSlot.EARLY_MORNING
        }
    }
}
