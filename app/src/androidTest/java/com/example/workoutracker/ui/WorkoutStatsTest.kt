package com.example.workoutracker.ui

import com.example.workoutracker.data.ExerciseEntity
import com.example.workoutracker.data.WorkoutExerciseEntity
import com.example.workoutracker.data.WorkoutExerciseWithSets
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

    // ---- finishing ----

    private fun exercise(id: Long, vararg sets: WorkoutSetEntity) = WorkoutExerciseWithSets(
        workoutExercise = WorkoutExerciseEntity(id = id, workoutId = 1, exerciseId = id, position = id.toInt()),
        sets = sets.toList(),
        exercise = ExerciseEntity(id = id, name = "Exercise $id")
    )

    @Test
    fun loggedSet_needsReps() {
        assertEquals(true, isLoggedSet(set(60.0, 8)))
        assertEquals(true, isLoggedSet(set(null, 12)))   // bodyweight
        assertEquals(false, isLoggedSet(set(60.0, null))) // weight only: not done yet
        assertEquals(false, isLoggedSet(set(null, null)))
    }

    @Test
    fun summary_countsWhatWillBeKeptAndRemoved() {
        val summary = summarizeForFinish(
            listOf(
                exercise(1, set(60.0, 8), set(60.0, null)),  // 1 logged, 1 removed
                exercise(2, set(null, null)),                // whole exercise removed
                exercise(3)                                  // no sets: removed
            )
        )
        assertEquals(FinishSummary(loggedSets = 1, setsToRemove = 2, exercisesToRemove = 2), summary)
        assertEquals(false, summary.hasNothingLogged)
    }

    @Test
    fun summary_ofEmptyWorkout_hasNothingLogged() {
        assertEquals(true, summarizeForFinish(emptyList()).hasNothingLogged)
        assertEquals(true, summarizeForFinish(listOf(exercise(1, set(60.0, null)))).hasNothingLogged)
    }

    @Test
    fun cleanupMessage_readsNaturally() {
        assertEquals(
            "1 set without reps will be removed.",
            finishCleanupMessage(FinishSummary(loggedSets = 3, setsToRemove = 1, exercisesToRemove = 0))
        )
        assertEquals(
            "2 sets without reps and 1 empty exercise will be removed.",
            finishCleanupMessage(FinishSummary(loggedSets = 3, setsToRemove = 2, exercisesToRemove = 1))
        )
        assertEquals(
            "2 empty exercises will be removed.",
            finishCleanupMessage(FinishSummary(loggedSets = 3, setsToRemove = 0, exercisesToRemove = 2))
        )
    }
}