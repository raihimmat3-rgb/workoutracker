package com.example.workoutracker.ui

import com.example.workoutracker.data.ExerciseEntity
import com.example.workoutracker.data.WorkoutExerciseEntity
import com.example.workoutracker.data.WorkoutExerciseWithSets
import com.example.workoutracker.data.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutStatsTest {

    private fun set(kg: Double?, reps: Int?, done: Boolean = true) =
        WorkoutSetEntity(workoutExerciseId = 1, setIndex = 0, weightKg = kg, reps = reps, isDone = done)

    private fun exercise(id: Long, vararg sets: WorkoutSetEntity) = WorkoutExerciseWithSets(
        workoutExercise = WorkoutExerciseEntity(id = id, workoutId = 1, exerciseId = id, position = id.toInt()),
        sets = sets.toList(),
        exercise = ExerciseEntity(id = id, name = "Exercise $id")
    )

    // ---- volume ----

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
    fun exerciseVolume_countsOnlyDoneSets() {
        val sets = listOf(set(60.0, 8), set(60.0, 7), set(100.0, 5, done = false), set(null, 10))
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

    // ---- ticking ----

    @Test
    fun canTick_needsReps() {
        assertEquals(true, canTick("8"))
        assertEquals(false, canTick(""))
    }

    // ---- finishing ----

    @Test
    fun summary_countsDoneNotDoneAndEmptyExercises() {
        val summary = summarizeForFinish(
            listOf(
                exercise(1, set(60.0, 8), set(60.0, 8, done = false)),
                exercise(2, set(null, null, done = false)),
                exercise(3) // no sets at all
            )
        )
        assertEquals(FinishSummary(doneSets = 1, notDoneSets = 2, emptyExercises = 1), summary)
        assertEquals(false, summary.hasNothingDone)
    }

    @Test
    fun summary_withNoTickedSets_hasNothingDone() {
        assertEquals(true, summarizeForFinish(emptyList()).hasNothingDone)
        assertEquals(true, summarizeForFinish(listOf(exercise(1, set(60.0, 8, done = false)))).hasNothingDone)
    }

    @Test
    fun finishNotes_readNaturally() {
        assertEquals(emptyList<String>(), finishNotes(FinishSummary(3, 0, 0)))
        assertEquals(
            listOf("1 set isn't ticked and will be saved as not done."),
            finishNotes(FinishSummary(3, 1, 0))
        )
        assertEquals(
            listOf(
                "2 sets aren't ticked and will be saved as not done.",
                "2 exercises with no sets will be removed."
            ),
            finishNotes(FinishSummary(3, 2, 2))
        )
    }
}