package com.example.workoutracker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Plain JVM unit tests: they run on your computer in a second, no phone needed. */
class ExerciseValidationTest {

    @Test
    fun emptyName_isRejected() {
        assertEquals("Enter a name", validateExerciseName(""))
    }

    @Test
    fun whitespaceOnlyName_isRejected() {
        assertEquals("Enter a name", validateExerciseName("   \t  "))
    }

    @Test
    fun nameOf61Characters_isTooLong() {
        assertNotNull(validateExerciseName("a".repeat(61)))
    }

    @Test
    fun nameOf60Characters_isAccepted() {
        assertNull(validateExerciseName("a".repeat(60)))
    }

    @Test
    fun normalName_isAccepted() {
        assertNull(validateExerciseName("Bench Press (Barbell)"))
    }

    @Test
    fun surroundingWhitespace_isIgnoredWhenCountingLength() {
        // 60 real characters plus padding should still pass, because the name is trimmed first.
        assertNull(validateExerciseName("  " + "a".repeat(60) + "  "))
    }
}