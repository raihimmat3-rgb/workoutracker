package com.example.workoutracker.hevy

import com.example.workoutracker.data.ExerciseEntity
import com.example.workoutracker.data.WorkoutExerciseWithSets
import com.example.workoutracker.data.WorkoutWithDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HevyMappingTest {

    private val hevy = HevyWorkout(
        title = "push, heavy", startedAt = 1_000, endedAt = 9_000, description = "felt strong",
        exercises = listOf(
            HevyExercise("Bench Press (Barbell)", null, listOf(
                HevySet(0, "warmup", 60.0, 10, null, null, null),
                HevySet(1, "normal", 27.2, 6, null, null, 8.5)
            )),
            HevyExercise("Hanging Leg Raise", "slow", listOf(HevySet(0, "normal", null, 12, null, null, null)))
        )
    )

    /** Builds what HevySync would write, as the database would hand it back. */
    private fun storedVersion(libraryNames: List<String>): WorkoutWithDetails {
        val workout = hevy.toNewWorkoutEntity(hevy.toContent().fingerprint()).copy(id = 7)
        val exercises = hevy.exercises.mapIndexed { pos, e ->
            val we = e.toWorkoutExerciseEntity(workoutId = 7, exerciseId = pos + 1L, position = pos).copy(id = pos + 100L)
            WorkoutExerciseWithSets(
                workoutExercise = we,
                sets = e.sets.mapIndexed { i, s -> s.toSetEntity(we.id, i) }.reversed(), // DB order isn't guaranteed
                exercise = ExerciseEntity(id = pos + 1L, name = libraryNames[pos])
            )
        }
        return WorkoutWithDetails(workout, exercises.reversed())
    }

    @Test
    fun anImportedWorkoutFingerprintsExactlyLikeTheFile() {
        val stored = storedVersion(listOf("Bench Press (Barbell)", "Hanging Leg Raise"))
        assertEquals(hevy.toContent().fingerprint(), stored.toContent().fingerprint())
    }

    @Test
    fun stillMatchesWhenTheLibraryNameDiffersOnlyInCase() {
        // An existing library exercise "bench press (barbell)" is reused for Hevy's "Bench Press (Barbell)".
        val stored = storedVersion(listOf("bench press (barbell)", "Hanging Leg Raise"))
        assertEquals(hevy.toContent().fingerprint(), stored.toContent().fingerprint())
    }

    @Test
    fun newWorkoutRowIsMarkedAsFromHevy() {
        val row = hevy.toNewWorkoutEntity("abc")
        assertEquals("hevy", row.source)
        assertEquals(1_000L, row.hevyKey)
        assertEquals("abc", row.hevyFingerprint)
        assertEquals("abc", row.syncedFingerprint)
        assertEquals("felt strong", row.notes)
    }

    @Test
    fun importedSetsAreTickedAndKeepTheirDetails() {
        val set = hevy.exercises[0].sets[1].toSetEntity(workoutExerciseId = 5, setIndex = 1)
        assertTrue(set.isDone)
        assertEquals(8.5, set.rpe!!, 0.0001)
        assertEquals("normal", set.setType)
        assertEquals("warmup", hevy.exercises[0].sets[0].toSetEntity(5, 0).setType)
    }
}