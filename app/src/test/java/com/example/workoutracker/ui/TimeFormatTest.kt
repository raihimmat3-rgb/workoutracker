package com.example.workoutracker.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun zero_isDoubleZero() {
        assertEquals("00:00", formatElapsed(0))
    }

    @Test
    fun underAnHour_showsMinutesAndSeconds() {
        assertEquals("05:07", formatElapsed((5 * 60 + 7) * 1000L))
    }

    @Test
    fun overAnHour_showsHours() {
        assertEquals("1:05:07", formatElapsed((3600 + 5 * 60 + 7) * 1000L))
    }

    @Test
    fun negative_isTreatedAsZero() {
        assertEquals("00:00", formatElapsed(-5_000))
    }

    @Test
    fun partialSeconds_areRoundedDown() {
        assertEquals("00:01", formatElapsed(1_999))
    }

    @Test
    fun defaultTitle_followsTimeOfDay() {
        assertEquals("Night workout", defaultWorkoutTitle(4))
        assertEquals("Morning workout", defaultWorkoutTitle(5))
        assertEquals("Morning workout", defaultWorkoutTitle(11))
        assertEquals("Afternoon workout", defaultWorkoutTitle(12))
        assertEquals("Afternoon workout", defaultWorkoutTitle(16))
        assertEquals("Evening workout", defaultWorkoutTitle(17))
        assertEquals("Evening workout", defaultWorkoutTitle(21))
        assertEquals("Night workout", defaultWorkoutTitle(22))
        assertEquals("Night workout", defaultWorkoutTitle(0))
    }
}