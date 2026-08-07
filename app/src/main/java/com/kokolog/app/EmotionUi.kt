package com.kokolog.app

import androidx.compose.ui.graphics.Color

/**
 * 感情表示用のデータ構造
 *
 * label : 感情名
 * icon  : 感情アイコン
 * color : 感情の背景色
 */
data class EmotionUi(
    val label: String,
    val icon: String,
    val color: Color
)

/**
 * アプリで使う感情一覧
 *
 * ここを変更すれば、
 * 記録画面・一覧画面・詳細画面の感情表示をまとめて調整できる
 */
val emotionUiList = listOf(
    EmotionUi("ふつう", "🌿", Color(0xFFE8E1CF)),
    EmotionUi("あんしん", "🫖", Color(0xFFDCE8D2)),
    EmotionUi("すっきり", "✨", Color(0xFFDDF4EA)),
    EmotionUi("うれしい", "😊", Color(0xFFFFF3B8)),
    EmotionUi("たのしい", "🎈", Color(0xFFFFE3C7)),
    EmotionUi("もやもや", "☁️", Color(0xFFE6DDEE)),
    EmotionUi("ふあん", "🌧️", Color(0xFFD7DDEC)),
    EmotionUi("しんぱい", "💭", Color(0xFFE1DFEA)),
    EmotionUi("かなしい", "☔", Color(0xFFC9DDF2)),
    EmotionUi("あせり", "⏳", Color(0xFFF0D8C8)),
    EmotionUi("いらいら", "🌋", Color(0xFFF4CACA)),
    EmotionUi("つかれた", "🫧", Color(0xFFD3E0EB))
)

/**
 * 感情名からアイコンを取得する
 */
fun emotionIcon(emotion: String): String {
    return emotionUiList
        .firstOrNull { it.label == emotion }
        ?.icon
        ?: "📝"
}

/**
 * 感情名から背景色を取得する
 */
fun emotionColor(emotion: String): Color {
    return emotionUiList
        .firstOrNull { it.label == emotion }
        ?.color
        ?: Color(0xFFE6E6E6)
}