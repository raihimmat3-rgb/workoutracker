package com.example.workoutracker.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayFormatTest {

    private fun minutes(m: Long) = m * 60_000

    @Test
    fun duration_underAMinute() {
        assertEquals("<1m", formatDuration(0))
        assertEquals("<1m", formatDuration(59_999))
    }

    @Test
    fun duration_minutesOnly() {
        assertEquals("45m", formatDuration(minutes(45)))
    }

    @Test
    fun duration_wholeHours_dropZeroMinutes() {
        assertEquals("1h", formatDuration(minutes(60)))
        assertEquals("2h", formatDuration(minutes(120)))
    }

    @Test
    fun duration_hoursAndMinutes() {
        assertEquals("1h 5m", formatDuration(minutes(65)))
        assertEquals("1h 30m", formatDuration(minutes(90)))
    }

    @Test
    fun duration_negative_isTreatedAsZero() {
        assertEquals("<1m", formatDuration(-1_000))
    }

    @Test
    fun countLabel_pluralises() {
        assertEquals("0 sets", countLabel(0, "set"))
        assertEquals("1 set", countLabel(1, "set"))
        assertEquals("7 exercises", countLabel(7, "exercise"))
    }
}