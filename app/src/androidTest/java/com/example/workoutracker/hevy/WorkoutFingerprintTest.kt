package com.example.workoutracker.hevy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutFingerprintTest {

    private fun content(
        title: String = "Push",
        exercise: String = "Bench Press (Barbell)",
        weight: Double? = 100.0,
        reps: Int? = 5,
        done: Boolean = true
    ) = WorkoutContent(
        title = title, endedAt = 5_000, notes = null,
        exercises = listOf(
            ExerciseContent(exercise, null, listOf(SetContent("normal", weight, reps, null, null, null, done)))
        )
    )

    @Test
    fun sameContent_sameFingerprint() {
        assertEquals(content().fingerprint(), content().fingerprint())
    }

    @Test
    fun anyRealChange_changesIt() {
        val base = content().fingerprint()
        assertTrue(base != content(reps = 6).fingerprint())
        assertTrue(base != content(weight = 102.5).fingerprint())
        assertTrue(base != content(title = "Push day").fingerprint())
        assertTrue(base != content(done = false).fingerprint()) // unticking a set counts as an edit
    }

    @Test
    fun ignoresExerciseNameCaseAndFloatingPointNoise() {
        assertEquals(content(weight = 27.2).fingerprint(), content(weight = 27.200000000000003).fingerprint())
        assertEquals(content().fingerprint(), content(exercise = "bench press (barbell)").fingerprint())
    }

    @Test
    fun hevyWorkoutsCountAsDone() {
        val hevy = HevyWorkout(
            title = "Push", startedAt = 1_000, endedAt = 5_000, description = null,
            exercises = listOf(HevyExercise("Bench Press (Barbell)", null, listOf(HevySet(0, "normal", 100.0, 5, null, null, null))))
        )
        assertEquals(content().fingerprint(), hevy.toContent().fingerprint())
    }
}