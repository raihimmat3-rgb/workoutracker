package com.example.workoutracker.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class WorkoutTimesTest {

    private val london = ZoneId.of("Europe/London")
    private fun londonMillis(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(london).toInstant().toEpochMilli()
    private fun utcMidnight(y: Int, mo: Int, d: Int) =
        LocalDateTime.of(y, mo, d, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun shift_keepsTheDuration() {
        val (start, end) = shiftWorkoutTimes(startedAt = 10_000, endedAt = 70_000, newStartedAt = 4_000)
        assertEquals(4_000L, start)
        assertEquals(64_000L, end)
        assertEquals(60_000L, end - start)
    }

    @Test
    fun pickerDate_isUtcMidnightOfTheLocalDay() {
        // 00:30 on 1 Aug in London is still 31 July in UTC (summer time), but the
        // workout happened on 1 Aug for you, so the picker must open on 1 Aug.
        assertEquals(utcMidnight(2026, 8, 1), toPickerDateMillis(londonMillis(2026, 8, 1, 0, 30), london))
    }

    @Test
    fun combine_buildsTheLocalTimeOnThePickedDay() {
        assertEquals(
            londonMillis(2026, 11, 3, 19, 45),
            combineDateAndTime(utcMidnight(2026, 11, 3), 19, 45, london)
        )
    }

    @Test
    fun combine_handlesSummerTime() {
        assertEquals(
            londonMillis(2026, 7, 14, 7, 5),
            combineDateAndTime(utcMidnight(2026, 7, 14), 7, 5, london)
        )
    }
}