package com.example.workoutracker.ui

import com.example.workoutracker.data.WorkoutSetEntity
import kotlin.math.roundToLong

/** Weight x reps for one set, or 0 if either box is empty (e.g. a bodyweight set). */
fun setVolume(weightKg: Double?, reps: Int?): Double =
    if (weightKg == null || reps == null) 0.0 else weightKg * reps

/** Total volume of an exercise's sets, skipping sets with an empty box. */
fun exerciseVolume(sets: List<WorkoutSetEntity>): Double =
    sets.sumOf { setVolume(it.weightKg, it.reps) }

/**
 * Rounds to 1 decimal place first, because adding decimals can leave tiny errors
 * (0.1 + 0.2 = 0.30000000000000004), then formats like a weight: 960.0 -> "960".
 */
fun formatVolume(volume: Double): String =
    formatWeight((volume * 10).roundToLong() / 10.0)

/** Text for the "Previous" column: "60kg × 8", "60kg", "12 reps", or "—" if there's nothing. */
fun formatPrevious(weightKg: Double?, reps: Int?): String = when {
    weightKg != null && reps != null -> "${formatWeight(weightKg)}kg × $reps"
    weightKg != null -> "${formatWeight(weightKg)}kg"
    reps != null -> "$reps reps"
    else -> "—"
}