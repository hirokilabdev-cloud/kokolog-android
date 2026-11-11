package com.kokolog.app

import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * 月次詳細レポートの品質検証用ダミーデータ。
 *
 * 注意：全データはメモリ上で作成し、Roomには書き込まない。
 * "1日1件を30日" を標準として設計する。
 * 仕事・家族・趣味などのカテゴリーと感情が自然に分散するように固定している。
 */
object MonthlyReportQualityFixtures {
    private val tokyo: TimeZone = TimeZone.getTimeZone("Asia/Tokyo")

    data class Scenario(
        val title: String,
        val logs: List<EmotionLog>,
        val period: ReportPeriod
    )

    private data class Sample(
        val emotion: String,
        val category: String,
        val tag: String,
        val intensity: Int,
        val hour: Int,
        val memo: String
    )

    private fun millis(year: Int, month: Int, day: Int, hour: Int): Long =
        GregorianCalendar(tokyo).apply {
            clear()
            set(year, month - 1, day, hour, 0, 0)
        }.timeInMillis

    private fun record(id: Long, year: Int, month: Int, day: Int, sample: Sample) = EmotionLog(
        id = id,
        emotion = sample.emotion,
        category = sample.category,
        tags = sample.tag,
        isIgnored = false,
        intensity = sample.intensity,
        memo = sample.memo,
        createdAt = millis(year, month, day, sample.hour)
    )

    /**
     * 2026年10月：1日1件×30日。仕事11件、家族11件、趣味5件、その他3件。
     * 会議6件はいずれも「ふあん」で、詳細分析で根拠付きの違いを見つける例。
     * メモの文面はv1.0では解釈せず、将来の検証素材として残す。
     */
    private val standardSamples = listOf(
        Sample("ふあん", "仕事", "会議", 4, 9, "明日の会議で説明できるか心配"),
        Sample("うれしい", "家族", "子ども", 3, 19, "子どもとゆっくり話せた"),
        Sample("たのしい", "趣味", "ゲーム", 3, 21, "好きなゲームを楽しめた"),
        Sample("あんしん", "家族", "家族", 3, 18, "家で落ち着いた時間"),
        Sample("ふつう", "その他", "日常", 2, 15, "いつもどおりの日"),
        Sample("ふあん", "仕事", "会議", 4, 9, "質問に答えられるか不安"),
        Sample("うれしい", "家族", "子ども", 4, 20, "一緒に遊んだ"),
        Sample("もやもや", "仕事", "仕様", 3, 18, "要件がまだ固まらない"),
        Sample("あんしん", "家族", "家族", 2, 20, "家族で過ごせた"),
        Sample("ふあん", "仕事", "会議", 5, 9, "準備が間に合うかプレッシャー"),
        Sample("たのしい", "趣味", "ゲーム", 4, 22, "友人と遊べた"),
        Sample("うれしい", "家族", "子ども", 3, 12, "子どもの成長を感じた"),
        Sample("ふあん", "仕事", "会議", 4, 9, "発表のことを考えると緊張"),
        Sample("あんしん", "家族", "家族", 3, 20, "家で一息つけた"),
        Sample("つかれた", "仕事", "残業", 4, 21, "今日は残業で疲れた"),
        Sample("うれしい", "家族", "子ども", 4, 20, "笑顔を見ることができた"),
        Sample("たのしい", "趣味", "筋トレ", 3, 20, "トレーニングできた"),
        Sample("ふつう", "その他", "日常", 2, 18, "特に大きな出来事なし"),
        Sample("ふあん", "仕事", "会議", 4, 9, "質問が多くて緊張する"),
        Sample("うれしい", "家族", "子ども", 4, 18, "公園で遊べた"),
        Sample("ふあん", "仕事", "会議", 5, 9, "今日の会議で失敗しないか不安"),
        Sample("あんしん", "仕事", "準備", 3, 9, "事前に資料を確認できて落ち着いた"),
        Sample("うれしい", "家族", "子ども", 5, 19, "一緒に笑った"),
        Sample("すっきり", "趣味", "筋トレ", 3, 20, "運動で気分転換"),
        Sample("ふつう", "その他", "日常", 3, 14, "静かな一日"),
        Sample("もやもや", "仕事", "仕様", 3, 18, "調整が長引く"),
        Sample("あんしん", "家族", "家族", 3, 20, "家族と話せて安心"),
        Sample("つかれた", "仕事", "残業", 4, 20, "仕事の終わりが遅かった"),
        Sample("うれしい", "家族", "子ども", 3, 19, "嬉しい出来事があった"),
        Sample("たのしい", "趣味", "ゲーム", 4, 21, "自分の時間を楽しめた")
    )

    /** 比較用の2026年9月：1日1件×30日（ふあん12件）。 */
    private fun previousMonth30(): List<EmotionLog> = (1..30).map { day ->
        val emotion = when {
            day <= 12 -> "ふあん"
            day % 3 == 0 -> "ふつう"
            day % 3 == 1 -> "うれしい"
            else -> "あんしん"
        }
        val category = when (day % 3) {
            0 -> "仕事"
            1 -> "家族"
            else -> "趣味"
        }
        record(
            id = 1000L + day, year = 2026, month = 9, day = day,
            sample = Sample(emotion, category, "", if (emotion == "ふあん") 4 else 3,
                if (category == "仕事") 9 else 19, "前月の比較用ダミー記録")
        )
    }

    fun standard30WithPrevious30(): Scenario = Scenario(
        title = "標準：30件・30日（前月も30件）",
        logs = previousMonth30() + standardSamples.mapIndexed { index, sample ->
            record(index + 1L, 2026, 10, index + 1, sample)
        },
        period = ReportPeriod.month(2026, 10, tokyo)
    )

    /** 20件・15日。毎日記録しなくても振り返れるかを検証。 */
    fun realistic20Across15Days(): Scenario {
        val extraDays = listOf(2, 6, 9, 12, 15)
        val first15 = (1..15).map { day -> record(2000L + day, 2026, 10, day * 2, standardSamples[day - 1]) }
        val extras = extraDays.mapIndexed { index, day ->
            record(3000L + index, 2026, 10, day * 2, standardSamples[15 + index].copy(hour = 21))
        }
        return Scenario("現実的：20件・15日", first15 + extras, ReportPeriod.month(2026, 10, tokyo))
    }

    /** 60件・25日。1日に2〜3件残すユーザーの検証。 */
    fun frequent60Across25Days(): Scenario {
        val logs = buildList {
            var id = 4000L
            for (day in 1..25) {
                val count = if (day <= 10) 3 else 2
                for (slot in 0 until count) {
                    val s = standardSamples[(day + slot * 7 - 1) % standardSamples.size]
                    add(record(id++, 2026, 10, day, s.copy(hour = listOf(8, 13, 20)[slot])))
                }
            }
        }
        return Scenario("高頻度：60件・25日", logs, ReportPeriod.month(2026, 10, tokyo))
    }

    /** 19件・1日。件数だけを見て『傾向あり』と判断してはいけない例。 */
    fun concentrated19OnOneDay(): Scenario = Scenario(
        title = "偏り：19件・1日",
        logs = (1..19).map { n ->
            record(5000L + n, 2026, 10, 18,
                Sample("ふつう", "その他", "", 3, 10, "同じ日にまとめて入力したテストデータ"))
        },
        period = ReportPeriod.month(2026, 10, tokyo)
    )

    /** 30日間すべて同じ感情・カテゴリー。実データが十分でも発見がない例。 */
    fun uniform30NoDistinctInsight(): Scenario = Scenario(
        title = "単一傾向：30件・30日",
        logs = (1..30).map { day ->
            record(6000L + day, 2026, 10, day,
                Sample("ふつう", "その他", "", 3, 12, "いつもどおり"))
        },
        period = ReportPeriod.month(2026, 10, tokyo)
    )
}
