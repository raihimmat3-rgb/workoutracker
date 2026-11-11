package com.example.workoutracker.ui

import com.example.workoutracker.data.ExerciseEntity
import com.example.workoutracker.data.WorkoutEntity
import com.example.workoutracker.data.WorkoutExerciseEntity
import com.example.workoutracker.data.WorkoutExerciseWithSets
import com.example.workoutracker.data.WorkoutSetEntity
import com.example.workoutracker.data.WorkoutWithDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemplatePlanningTest {

    private fun set(kg: Double?, reps: Int?, done: Boolean = true) =
        WorkoutSetEntity(workoutExerciseId = 1, setIndex = 0, weightKg = kg, reps = reps, isDone = done)

    private fun item(exerciseId: Long, position: Int, vararg sets: WorkoutSetEntity) = WorkoutExerciseWithSets(
        workoutExercise = WorkoutExerciseEntity(id = exerciseId, workoutId = 1, exerciseId = exerciseId, position = position),
        sets = sets.toList(),
        exercise = ExerciseEntity(id = exerciseId, name = "Exercise $exerciseId")
    )

    @Test
    fun templateFromWorkout_keepsOrderAndCountsTickedSets() {
        val details = WorkoutWithDetails(
            workout = WorkoutEntity(id = 1, title = "Push", startedAt = 0, endedAt = 1),
            // Given out of order on purpose: position decides.
            exercises = listOf(
                item(20, position = 1, set(15.0, 12), set(15.0, 12, done = false)),
                item(10, position = 0, set(60.0, 8), set(60.0, 8), set(60.0, 6))
            )
        )

        val planned = templateExercisesFrom(details, templateId = 5)

        assertEquals(listOf(10L, 20L), planned.map { it.exerciseId })
        assertEquals(listOf(0, 1), planned.map { it.position })
        assertEquals(listOf(3, 1), planned.map { it.setCount })
        assertEquals(listOf(5L, 5L), planned.map { it.templateId })
    }

    @Test
    fun templateFromWorkout_neverPlansZeroSets() {
        val details = WorkoutWithDetails(
            workout = WorkoutEntity(id = 1, title = "w", startedAt = 0, endedAt = 1),
            exercises = listOf(item(10, 0, set(60.0, 8, done = false)))
        )
        assertEquals(1, templateExercisesFrom(details, 1).single().setCount)
    }

    @Test
    fun plannedSets_copyLastTimeSetBySet() {
        val previous = listOf(set(60.0, 8), set(60.0, 7))

        val sets = plannedSets(setCount = 3, previous = previous, workoutExerciseId = 9)

        assertEquals(listOf(8, 7, 7), sets.map { it.reps })        // the 3rd copies the last one
        assertEquals(listOf(0, 1, 2), sets.map { it.setIndex })
        assertEquals(true, sets.all { !it.isDone && it.workoutExerciseId == 9L })
    }

    @Test
    fun plannedSets_startEmptyWithNoHistory() {
        val sets = plannedSets(setCount = 2, previous = emptyList(), workoutExerciseId = 9)
        assertEquals(2, sets.size)
        assertNull(sets[0].weightKg)
        assertNull(sets[0].reps)
    }
}