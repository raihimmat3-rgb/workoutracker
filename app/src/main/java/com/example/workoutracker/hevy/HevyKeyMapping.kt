package com.example.workoutracker.hevy

import com.example.workoutracker.data.WorkoutEntity
import com.example.workoutracker.data.WorkoutExerciseEntity
import com.example.workoutracker.data.WorkoutSetEntity
import com.example.workoutracker.data.WorkoutWithDetails

/*
 * Conversions between Hevy's model and our database rows. Kept free of database calls so they
 * can be unit-tested, including the most important check of all: a workout imported from Hevy
 * must fingerprint EXACTLY the same as the file it came from, or every re-sync would think
 * you'd edited everything.
 */

/** A workout as stored in the app, in the shape used for fingerprints. */
fun WorkoutWithDetails.toContent(): WorkoutContent = WorkoutContent(
    title = workout.title,
    endedAt = workout.endedAt,
    notes = workout.notes,
    exercises = exercises
        .sortedBy { it.workoutExercise.position }
        .map { item ->
            ExerciseContent(
                name = item.exercise.name,
                notes = item.workoutExercise.notes,
                sets = item.sets.sortedBy { it.setIndex }.map { s ->
                    SetContent(s.setType, s.weightKg, s.reps, s.distanceMeters, s.durationSeconds, s.rpe, s.isDone)
                }
            )
        }
)

/** A brand-new workout row for a Hevy workout. Both fingerprints start as the file's. */
fun HevyWorkout.toNewWorkoutEntity(fingerprint: String) = WorkoutEntity(
    title = title,
    startedAt = startedAt,
    endedAt = endedAt,
    notes = description,
    source = "hevy",
    hevyKey = startedAt,
    hevyFingerprint = fingerprint,
    syncedFingerprint = fingerprint
)

fun HevyExercise.toWorkoutExerciseEntity(workoutId: Long, exerciseId: Long, position: Int) =
    WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, position = position, notes = notes)

/** Hevy sets are all done: you only export workouts you finished. */
fun HevySet.toSetEntity(workoutExerciseId: Long, setIndex: Int) = WorkoutSetEntity(
    workoutExerciseId = workoutExerciseId,
    setIndex = setIndex,
    setType = type,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    durationSeconds = durationSeconds,
    distanceMeters = distanceMeters,
    isDone = true
)