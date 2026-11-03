package com.example.workoutracker.ui

import com.example.workoutracker.data.WorkoutExerciseWithSets
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

/**
 * The rule for what counts when finishing: a set is "logged" once it has reps.
 * Weight alone isn't enough (you haven't done the set yet); reps alone is fine (bodyweight).
 * The finish transaction's DELETE queries follow the same rule, so keep them in step.
 */
fun isLoggedSet(set: WorkoutSetEntity): Boolean = set.reps != null

/** What finishing will keep and what it will tidy away. Shown in the Finish dialog. */
data class FinishSummary(
    val loggedSets: Int,
    val setsToRemove: Int,
    val exercisesToRemove: Int
) {
    val hasNothingLogged: Boolean get() = loggedSets == 0
}

fun summarizeForFinish(exercises: List<WorkoutExerciseWithSets>): FinishSummary {
    val allSets = exercises.flatMap { it.sets }
    val logged = allSets.count(::isLoggedSet)
    return FinishSummary(
        loggedSets = logged,
        setsToRemove = allSets.size - logged,
        exercisesToRemove = exercises.count { item -> item.sets.none(::isLoggedSet) }
    )
}

/** e.g. "2 sets without reps and 1 empty exercise will be removed." */
fun finishCleanupMessage(summary: FinishSummary): String {
    fun plural(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"
    val parts = buildList {
        if (summary.setsToRemove > 0) add(plural(summary.setsToRemove, "set") + " without reps")
        if (summary.exercisesToRemove > 0) add(plural(summary.exercisesToRemove, "empty exercise"))
    }
    return parts.joinToString(" and ") + " will be removed."
}