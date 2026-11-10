package com.example.workoutracker.data

/**
 * One workout's numbers for one exercise (a row of observeExerciseSessions), used by the chart.
 * Weight-based values are null when every ticked set was bodyweight.
 */
data class ExerciseSession(
    val workoutId: Long,
    val startedAt: Long,
    val heaviestKg: Double?,
    val bestOneRepMaxKg: Double?,
    val volumeKg: Double,
    val mostReps: Int?
)