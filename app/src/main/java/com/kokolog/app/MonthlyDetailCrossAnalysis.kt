package com.kokolog.app

import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * STEP4.3-A: 月30件程度の記録でも、実際に記録された組み合わせだけを紹介する。
 *
 * 感情の良し悪し・性格・原因・回復を点数化しない。
 * RoomやReportAnalyzerの販売可否判定は変更せず、詳細画面の補足情報に使う。
 */
enum class CrossSource { CATEGORY, TAG }

data class MonthlyCrossFinding(
    val source: CrossSource,
    val groupName: String,
    val title: String,
    val message: String,
    val evidenceLogIds: List<Long>
)

object MonthlyDetailCrossAnalysis {
    fun findings(
        allLogs: List<EmotionLog>,
        period: ReportPeriod,
        maxCount: Int = 2
    ): List<MonthlyCrossFinding> {
        if (maxCount <= 0) return emptyList()
        val month = allLogs.filter { it.createdAt >= period.startMillis && it.createdAt < period.endExclusiveMillis }
        val tz = TimeZone.getTimeZone(period.timeZoneId)
        val recordedDays = month.map { calendarDate(it.createdAt, tz) }.toSet().size
        // 19件が1日だけに集中したデータからは「傾向」を作らない。
        if (month.size < 20 || recordedDays < 7) return emptyList()

        val candidates = mutableListOf<Pair<Int, MonthlyCrossFinding>>()
        val categories = month.map { it.category }.toSet().sorted()
        val tags = month.flatMap { parseTags(it.tags) }.toSet().sorted()

        fun consider(source: CrossSource, name: String, selected: List<EmotionLog>) {
            if (selected.size < 5 || selected.size == month.size) return
            if (selected.map { calendarDate(it.createdAt, tz) }.toSet().size < 2) return
            val distribution = selected.groupingBy { it.emotion }.eachCount()
                .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            val top = distribution.firstOrNull() ?: return
            if (top.value < 3) return
            val second = distribution.drop(1).firstOrNull { it.value >= 3 }
            val others = month.filterNot { it in selected }
            val share = top.value.toDouble() / selected.size
            val otherShare = if (others.isEmpty()) 0.0 else others.count { it.emotion == top.key }.toDouble() / others.size
            val notablyDifferent = others.size >= 10 && share - otherShare >= 0.20
            // 同じテーマに複数の感情があるか、他のテーマとの割合差に裏付けがあるときだけ。
            if (second == null && !notablyDifferent) return

            val matching = selected.filter { it.emotion == top.key }
            val strong = matching.count { it.intensity in 4..5 }
            val isTag = source == CrossSource.TAG
            val sourceLabel = if (isTag) "#${name}" else "${name}"
            val first = "「${sourceLabel}」の記録${selected.size}件のうち、「${top.key}」は${top.value}件でした。"
            val contrast = when {
                second != null -> "同じテーマには「${second.key}」も${second.value}件残されています。"
                notablyDifferent -> "${if (isTag) "そのタグを付けていない記録" else "ほかのカテゴリー"}と比べ、「${top.key}」の割合が${toPercent(share - otherShare)}ポイント高く記録されました。"
                else -> ""
            }
            val intensity = if (matching.size >= 3 && strong > 0) {
                "「${top.key}」のうち、強さ4〜5は${strong}件です。"
            } else ""
            val body = listOf(first, contrast, intensity).filter { it.isNotBlank() }.joinToString(" ")
            val title = if (second != null) "同じテーマに残った、異なるココロ" else "テーマと感情の組み合わせ"
            // 同じテーマ全体を根拠にする。異なる感情の記録も読み返せるようにする。
            val ids = selected.filter { it.id > 0 }.sortedByDescending { it.createdAt }.map { it.id }.distinct()
            val priority = (if (second != null) 100 + minOf(second.value, 10) else 50) + selected.size
            candidates.add(priority to MonthlyCrossFinding(
                source = source,
                groupName = name,
                title = title,
                message = body,
                evidenceLogIds = ids
            ))
        }
        categories.forEach { category ->
            consider(CrossSource.CATEGORY, category, month.filter { it.category == category })
        }
        tags.forEach { tag ->
            consider(CrossSource.TAG, tag, month.filter { tag in parseTags(it.tags) })
        }
        // 同じデータをカテゴリーとタグで言い換えるだけにならないよう重複を抑える。
        val ordered = candidates.sortedWith(compareByDescending<Pair<Int, MonthlyCrossFinding>> { it.first }
            .thenBy { it.second.source.name }.thenBy { it.second.groupName })
        val chosen = mutableListOf<MonthlyCrossFinding>()
        fun addIfDistinct(item: MonthlyCrossFinding) {
            if (chosen.size >= maxCount || item in chosen) return
            val ids = item.evidenceLogIds.toSet()
            val overlaps = chosen.any { existing ->
                val old = existing.evidenceLogIds.toSet()
                ids.isNotEmpty() && old.isNotEmpty() &&
                    ids.intersect(old).size.toDouble() / minOf(ids.size, old.size) >= 0.8
            }
            if (!overlaps) chosen += item
        }
        // 同じ分析軸ばかりにならないようカテゴリーとタグを優先して一つずつ選ぶ。
        listOf(CrossSource.CATEGORY, CrossSource.TAG).forEach { source ->
            for ((_, item) in ordered) {
                if (item.source != source) continue
                val before = chosen.size
                addIfDistinct(item)
                if (chosen.size > before) break
            }
        }
        ordered.forEach { addIfDistinct(it.second) }
        return chosen
    }

    private fun calendarDate(time: Long, zone: TimeZone): String =
        GregorianCalendar(zone).apply { timeInMillis = time }.let {
            "${it.get(java.util.Calendar.YEAR)}-${it.get(java.util.Calendar.DAY_OF_YEAR)}"
        }

    private fun toPercent(part: Double): String =
        String.format(java.util.Locale.JAPAN, "%.1f", part * 100.0)

    /** 記録時のCSVタグを正規化。重複タグは1記録につき1回扱う。 */
    fun parseTags(csv: String): List<String> = csv.split(',')
        .map { it.trim().removePrefix("#").trim() }
        .filter { it.isNotEmpty() }.distinct()
}

/** 関連記録ダイアログの表示文言をUIから分離してテストできるようにする。 */
object MonthlyDetailRecordPreview {
    fun intensity(intensity: Int): String? = if (intensity in 1..5) "強さ $intensity/5" else null
    fun tags(raw: String): String {
        val items = MonthlyDetailCrossAnalysis.parseTags(raw)
        val shown = items.take(4).joinToString("  ") { "#$it" }
        return if (items.size <= 4) shown else "$shown  ほか${items.size - 4}タグ"
    }
    fun memo(raw: String, maxChars: Int = 85): String? {
        val normalized = raw.replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return null
        if (maxChars < 2) return normalized
        return if (normalized.length <= maxChars) normalized else normalized.take(maxChars - 1) + "…"
    }
}
