package com.example.workoutracker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetInputTest {

    @Test
    fun weight_acceptsNormalAndUnfinishedInput() {
        listOf("", "60", "22.5", "60.", "0.25", "179.5", "9999").forEach {
            assertTrue("should accept '$it'", isValidWeightInput(it))
        }
    }

    @Test
    fun weight_rejectsJunk() {
        listOf("6..", "abc", "1.234", "12345", "-5", "6,5", "6.5.").forEach {
            assertFalse("should reject '$it'", isValidWeightInput(it))
        }
    }

    @Test
    fun parseWeight_handlesEmptyAndUnfinished() {
        assertNull(parseWeight(""))
        assertNull(parseWeight("."))
        assertEquals(60.0, parseWeight("60.")!!, 0.0001)
        assertEquals(22.5, parseWeight("22.5")!!, 0.0001)
    }

    @Test
    fun reps_acceptsUpToThreeDigits() {
        listOf("", "8", "100").forEach { assertTrue("should accept '$it'", isValidRepsInput(it)) }
        listOf("8.5", "1000", "-1", "a").forEach { assertFalse("should reject '$it'", isValidRepsInput(it)) }
    }

    @Test
    fun parseReps_handlesEmpty() {
        assertNull(parseReps(""))
        assertEquals(12, parseReps("12"))
    }

    @Test
    fun formatWeight_dropsPointlessDecimals() {
        assertEquals("", formatWeight(null))
        assertEquals("60", formatWeight(60.0))
        assertEquals("22.5", formatWeight(22.5))
        assertEquals("0.25", formatWeight(0.25))
        assertEquals("179.5", formatWeight(179.5))
    }
}