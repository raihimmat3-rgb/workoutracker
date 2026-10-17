package com.example.workoutracker.data

import androidx.room.Embedded
import androidx.room.Relation

/** One exercise inside a workout, with its sets and the library exercise it refers to. */
data class WorkoutExerciseWithSets(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<WorkoutSetEntity>,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity
)

/** A whole workout: its exercises, and each exercise's sets. */
data class WorkoutWithDetails(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val exercises: List<WorkoutExerciseWithSets>
)