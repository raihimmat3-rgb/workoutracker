package com.example.workoutracker.ui

import com.example.workoutracker.data.TemplateExerciseEntity
import com.example.workoutracker.data.WorkoutSetEntity
import com.example.workoutracker.data.WorkoutWithDetails

/**
 * The exercise list for a routine made from a finished workout: same order, and as many sets
 * as you ticked (at least 1, so an exercise is never planned with no sets).
 */
fun templateExercisesFrom(details: WorkoutWithDetails, templateId: Long): List<TemplateExerciseEntity> =
    details.exercises
        .sortedBy { it.workoutExercise.position }
        .mapIndexed { position, item ->
            TemplateExerciseEntity(
                templateId = templateId,
                exerciseId = item.exercise.id,
                position = position,
                setCount = item.sets.count { it.isDone }.coerceAtLeast(1)
            )
        }

/**
 * The sets to create when starting a routine: [setCount] unticked sets, each pre-filled with
 * the weight and reps of the same set last time. If last time had fewer sets, the extra ones
 * copy its final set; with no history at all, they start empty.
 */
fun plannedSets(setCount: Int, previous: List<WorkoutSetEntity>, workoutExerciseId: Long): List<WorkoutSetEntity> =
    (0 until setCount).map { i ->
        val source = previous.getOrNull(i) ?: previous.lastOrNull()
        WorkoutSetEntity(
            workoutExerciseId = workoutExerciseId,
            setIndex = i,
            weightKg = source?.weightKg,
            reps = source?.reps,
            isDone = false
        )
    }