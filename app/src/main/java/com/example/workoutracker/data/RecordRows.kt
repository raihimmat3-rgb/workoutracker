package com.example.workoutracker.data

/**
 * One set picked out by a personal-best query, plus when it happened.
 * Not a table, just the shape of those queries' results.
 */
data class SetRecord(
    val weightKg: Double?,
    val reps: Int?,
    val startedAt: Long,
    val workoutId: Long
)

/** Result of observeExerciseStats(): lastPerformedAt is null if it's never been done. */
data class ExerciseStats(
    val workoutCount: Int,
    val lastPerformedAt: Long?
)