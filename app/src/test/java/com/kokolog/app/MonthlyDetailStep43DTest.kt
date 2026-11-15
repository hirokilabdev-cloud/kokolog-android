package com.kokolog.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/** STEP4.3-D: 実データには触れない純粋ロジックの検証。 */
class MonthlyDetailStep43DTest {
    private val zone = ZoneId.of("Asia/Tokyo")
    private val period = ReportPeriod.month(2026, 7, TimeZone.getTimeZone(zone))
    private fun time(day: Int, hour: Int = 12) =
        LocalDateTime.of(2026, 7, day, hour, 0).atZone(zone).toInstant().toEpochMilli()
    private fun log(id: Long, day: Int, ignored: Boolean = false) = EmotionLog(
        id = id, emotion = "しんぱい", category = "仕事", tags = "会議", intensity = 4,
        memo = "当時の記録", createdAt = time(day), isIgnored = ignored
    )
    private fun change(id: Long, originalId: Long, millis: Long) = EmotionChangeLog(
        id = id, originalLogId = originalId, emotion = "あんしん", intensity = 2,
        memo = "その後の気持ち", createdAt = millis
    )

    @Test fun selectedPastDateCarriesNowTime() {
        val now = time(28, 18)
        val result = CalendarRecordDatePolicy.initialTimestamp(LocalDate.of(2026, 7, 9), now, zone)
        assertEquals(time(9, 18), result)
    }
    @Test fun futureDateFallsBackToToday() {
        val now = time(28, 18)
        val result = CalendarRecordDatePolicy.initialTimestamp(LocalDate.of(2026, 8, 10), now, zone)
        assertEquals(now, result)
    }
    @Test fun nullDateKeepsExactCurrentTime() {
        val now = time(28, 18) + 2345L
        assertEquals(now, CalendarRecordDatePolicy.initialTimestamp(null, now, zone))
    }
    @Test fun multipleChangesForOneOriginalCountAsOneStory() {
        val records = listOf(log(1, 2))
        val changes = listOf(change(2, 1, time(3)), change(1, 1, time(2, 14)))
        val result = MonthlyAfterHeartPresentation.stories(records, changes, period)
        assertEquals(1, result.size)
        assertEquals(listOf(1L, 2L), result.first().events.map { it.id })
    }
    @Test fun outsidePeriodAndBeforeOriginalChangesExcluded() {
        val records = listOf(log(1, 2), log(2, 3))
        val outOfMonth = LocalDateTime.of(2026, 8, 1, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val changes = listOf(change(1, 1, time(1)), change(2, 1, outOfMonth), change(3, 2, time(4)))
        val result = MonthlyAfterHeartPresentation.stories(records, changes, period)
        assertEquals(listOf(2L), result.map { it.original.id })
        assertEquals(1, result.first().events.size)
    }
    @Test fun ignoredDoesNotRequireChanges() {
        val records = listOf(log(1, 5, true), log(2, 7), log(3, 6, true))
        assertEquals(listOf(3L, 1L), MonthlyAfterHeartPresentation.ignoredLogs(records, period).map { it.id })
        assertTrue(MonthlyAfterHeartPresentation.stories(records, emptyList(), period).isEmpty())
    }
    @Test fun missingOriginalNeverBecomesStory() {
        assertTrue(MonthlyAfterHeartPresentation.stories(emptyList(), listOf(change(1, 99, time(4))), period).isEmpty())
    }
}
