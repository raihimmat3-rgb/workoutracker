package com.example.workoutracker.ui

import com.example.workoutracker.data.SetRecord

/** The four records shown on an exercise's screen. Any of them is null until it's been done. */
data class PersonalBests(
    val heaviest: SetRecord?,
    val bestOneRepMax: SetRecord?,
    val mostReps: SetRecord?,
    val bestVolume: SetRecord?
) {
    val isEmpty: Boolean get() = listOf(heaviest, bestOneRepMax, mostReps, bestVolume).all { it == null }
}

/**
 * Estimated one-rep max using the Epley formula: weight x (1 + reps / 30).
 * One rep is exactly its weight. It gets less accurate above about 10-12 reps, so treat
 * high-rep estimates as rough. Must match the ORDER BY in observeBestOneRepMaxSet.
 */
fun estimateOneRepMax(weightKg: Double, reps: Int): Double = when {
    reps <= 0 -> 0.0
    reps == 1 -> weightKg
    else -> weightKg * (1 + reps / 30.0)
}