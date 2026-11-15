package com.kokolog.app

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * カレンダーから追加する記録の初期日時を決める。
 * 選択日が未来であれば今日、過去・今日なら選択日を使う。
 * 時刻はボタンを押した時点の時・分を引き継ぐ（未来日時にはしない）。
 * Roomに保存する日時の形式は従来の epoch milliseconds のまま。
 */
object CalendarRecordDatePolicy {
    fun initialTimestamp(
        selectedDay: LocalDate?,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        if (selectedDay == null) return nowMillis
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val day = if (selectedDay.isAfter(now.toLocalDate())) now.toLocalDate() else selectedDay
        return LocalDateTime.of(day, now.toLocalTime())
            .atZone(zoneId).toInstant().toEpochMilli()
    }
}
