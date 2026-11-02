package com.example.workoutracker.ui

import com.example.workoutracker.data.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutStatsTest {

    private fun set(kg: Double?, reps: Int?) =
        WorkoutSetEntity(workoutExerciseId = 1, setIndex = 0, weightKg = kg, reps = reps)

    @Test
    fun setVolume_isWeightTimesReps() {
        assertEquals(480.0, setVolume(60.0, 8), 0.0001)
    }

    @Test
    fun setVolume_isZeroWhenABoxIsEmpty() {
        assertEquals(0.0, setVolume(null, 12), 0.0001)
        assertEquals(0.0, setVolume(60.0, null), 0.0001)
    }

    @Test
    fun exerciseVolume_addsSetsAndSkipsEmptyOnes() {
        val sets = listOf(set(60.0, 8), set(60.0, 7), set(null, 10), set(62.5, null))
        assertEquals(900.0, exerciseVolume(sets), 0.0001)
    }

    @Test
    fun formatVolume_roundsAwayFloatingPointNoise() {
        assertEquals("0.3", formatVolume(0.1 + 0.2))
        assertEquals("960", formatVolume(960.0))
        assertEquals("67.5", formatVolume(22.5 * 3))
    }

    @Test
    fun formatPrevious_coversEveryCase() {
        assertEquals("25kg × 7", formatPrevious(25.0, 7))
        assertEquals("22.5kg × 10", formatPrevious(22.5, 10))
        assertEquals("60kg", formatPrevious(60.0, null))
        assertEquals("12 reps", formatPrevious(null, 12))
        assertEquals("—", formatPrevious(null, null))
    }
}