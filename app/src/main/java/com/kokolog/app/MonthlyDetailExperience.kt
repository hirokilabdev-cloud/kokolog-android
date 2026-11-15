package com.kokolog.app

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * STEP4.3-B: ルールベースで作る「ココロアルバム」と「小さなヒント」。
 *
 * 保存済みの感情を良い/悪いと評価しない。メモの意味解析・原因推定・性格診断は行わない。
 * 期間、強度、タグ、カテゴリー、記録日を使って、元の記録に戻れる入口を作る。
 * Room/DAOや詳細レポートの販売可否は一切変更しない。
 */
data class MonthlyHeartAlbum(
    val title: String,
    val description: String,
    val evidenceLogIds: List<Long>,
    val totalCount: Int
)

data class MonthlyGentleHint(
    val title: String,
    val observation: String,
    val question: String,
    val evidenceLogIds: List<Long>
)

object MonthlyDetailExperience {
    /** 強さ4〜5の記録と、複数日に登場するテーマを最大2冊のアルバムとして届ける。 */
    fun albums(allLogs: List<EmotionLog>, period: ReportPeriod): List<MonthlyHeartAlbum> {
        val month = monthLogs(allLogs, period)
        if (month.isEmpty()) return emptyList()
        val albums = mutableListOf<MonthlyHeartAlbum>()
        val strong = month.filter { it.intensity in 4..5 }
            .sortedWith(compareByDescending<EmotionLog> { it.intensity }.thenByDescending { it.createdAt })
        if (strong.isNotEmpty()) {
            albums += MonthlyHeartAlbum(
                title = "強く残ったココロ",
                description = "強さ4〜5で記録した${strong.size}件を集めました。どんな気持ちも、そのまま残しています。",
                evidenceLogIds = strong.map { it.id },
                totalCount = strong.size
            )
        }

        val zone = TimeZone.getTimeZone(period.timeZoneId)
        // ひとつの記録の中で同じタグは1回だけ数える。
        val tagGroups = month.flatMap { log ->
            MonthlyDetailCrossAnalysis.parseTags(log.tags).map { tag -> tag to log }
        }.groupBy({ it.first }, { it.second })
        val tag = tagGroups.entries
            .filter { (_, entries) -> isRepeatedTheme(entries, month.size, zone, 3) }
            .sortedWith(compareByDescending<Map.Entry<String, List<EmotionLog>>> { it.value.size }
                .thenBy { it.key })
            .firstOrNull()

        if (tag != null) {
            val entries = tag.value.sortedByDescending { it.createdAt }
            albums += MonthlyHeartAlbum(
                title = "繰り返し登場したテーマ  #${tag.key}",
                description = "#${tag.key} が付いた記録${entries.size}件。日をまたいで残したココロを並べました。",
                evidenceLogIds = entries.map { it.id },
                totalCount = entries.size
            )
        } else {
            // タグがない場合もカテゴリーを手掛かりにできる（全件同一カテゴリーは除外）。
            val category = month.groupBy { it.category.trim() }
                .entries.filter { it.key.isNotBlank() && isRepeatedTheme(it.value, month.size, zone, 4) }
                .sortedWith(compareByDescending<Map.Entry<String, List<EmotionLog>>> { it.value.size }
                    .thenBy { it.key }).firstOrNull()
            if (category != null) {
                val entries = category.value.sortedByDescending { it.createdAt }
                albums += MonthlyHeartAlbum(
                    title = "繰り返し登場したテーマ  ${category.key}",
                    description = "「${category.key}」の記録${entries.size}件。異なる日のココロを読み返せます。",
                    evidenceLogIds = entries.map { it.id },
                    totalCount = entries.size
                )
            }
        }
        return albums
    }

    /**
     * クロス分析から裏付けのある振り返りの問いを作る。最大2つ。
     * 販売可否を新規に判断しない。提供不可月に「有料なら発見がある」ように見せない。
     */
    fun hints(
        allLogs: List<EmotionLog>,
        period: ReportPeriod,
        crossFindings: List<MonthlyCrossFinding>,
        canOfferMonthlyDetail: Boolean
    ): List<MonthlyGentleHint> {
        if (!canOfferMonthlyDetail) return emptyList()
        val month = monthLogs(allLogs, period)
        if (month.size < 20) return emptyList()
        val byId = month.associateBy { it.id }
        val result = mutableListOf<MonthlyGentleHint>()
        for (finding in crossFindings) {
            val evidence = finding.evidenceLogIds.mapNotNull { byId[it] }.distinctBy { it.id }
            if (evidence.size < 5) continue
            val dayCount = evidence.map { dayKey(it.createdAt, period.timeZoneId) }.toSet().size
            if (dayCount < 2) continue
            val emotions = evidence.groupingBy { it.emotion }.eachCount()
                .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            val name = if (finding.source == CrossSource.TAG) "#${finding.groupName}" else finding.groupName
            val observation = if (emotions.size >= 2) {
                "「$name」について${evidence.size}件の記録があり、「${emotions[0].key}」${emotions[0].value}件、「${emotions[1].key}」${emotions[1].value}件などが残されていました。"
            } else {
                "「$name」について${evidence.size}件の記録があり、そのうち「${emotions.first().key}」は${emotions.first().value}件でした。"
            }
            val question = if (finding.source == CrossSource.TAG) {
                "次に似た出来事があったとき、どんな気持ちだったかを一度だけ残してみませんか。前の記録と見比べるきっかけになります。"
            } else {
                "同じテーマでも、違う気持ちが残された日はありましたか。それぞれの記録を読み返してみませんか。"
            }
            val ids = evidence.sortedByDescending { it.createdAt }.map { it.id }
            // ほぼ同じ記録を使う提案を2件並べない。
            val overlaps = result.any { earlier ->
                val old = earlier.evidenceLogIds.toSet()
                ids.toSet().intersect(old).size.toDouble() / minOf(ids.size, old.size) >= 0.8
            }
            if (overlaps) continue
            result += MonthlyGentleHint(
                title = if (finding.source == CrossSource.TAG) "よく登場した出来事を見比べる" else "同じテーマの、違う気持ちを見つめる",
                observation = observation,
                question = question,
                evidenceLogIds = ids
            )
            if (result.size == 2) break
        }
        return result
    }

    private fun monthLogs(allLogs: List<EmotionLog>, period: ReportPeriod): List<EmotionLog> =
        allLogs.filter { it.id > 0 && it.createdAt >= period.startMillis && it.createdAt < period.endExclusiveMillis }

    private fun isRepeatedTheme(
        entries: List<EmotionLog>, monthTotal: Int, zone: TimeZone, minimum: Int
    ): Boolean = entries.size >= minimum && entries.size < monthTotal &&
        entries.map { dayKey(it.createdAt, zone.id) }.toSet().size >= 3

    private fun dayKey(timestamp: Long, timeZoneId: String): String =
        GregorianCalendar(TimeZone.getTimeZone(timeZoneId)).apply { timeInMillis = timestamp }.let { calendar ->
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
        }
}
