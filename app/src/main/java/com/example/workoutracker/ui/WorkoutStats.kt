package com.example.workoutracker.ui

import com.example.workoutracker.data.WorkoutExerciseWithSets
import com.example.workoutracker.data.WorkoutSetEntity
import kotlin.math.roundToLong

/** Weight x reps for one set, or 0 if either box is empty (e.g. a bodyweight set). */
fun setVolume(weightKg: Double?, reps: Int?): Double =
    if (weightKg == null || reps == null) 0.0 else weightKg * reps

/** Total volume of an exercise: only ticked (done) sets count. */
fun exerciseVolume(sets: List<WorkoutSetEntity>): Double =
    sets.filter { it.isDone }.sumOf { setVolume(it.weightKg, it.reps) }

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

/** A set can only be ticked once it has reps (bodyweight sets need no weight). */
fun canTick(repsText: String): Boolean = parseReps(repsText) != null

/** What finishing will save, shown in the Finish dialog. */
data class FinishSummary(
    val doneSets: Int,
    val notDoneSets: Int,
    val emptyExercises: Int
) {
    val hasNothingDone: Boolean get() = doneSets == 0
}

/**
 * The finish rule: ticked sets count; unticked sets are kept but saved as not done;
 * exercises with no sets at all are removed (the finish transaction does the same in SQL).
 */
fun summarizeForFinish(exercises: List<WorkoutExerciseWithSets>): FinishSummary {
    val allSets = exercises.flatMap { it.sets }
    val done = allSets.count { it.isDone }
    return FinishSummary(
        doneSets = done,
        notDoneSets = allSets.size - done,
        emptyExercises = exercises.count { it.sets.isEmpty() }
    )
}

/** The extra lines for the Finish dialog, e.g. "2 sets aren't ticked and will be saved as not done." */
fun finishNotes(summary: FinishSummary): List<String> {
    fun plural(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"
    return buildList {
        if (summary.notDoneSets > 0) {
            val verb = if (summary.notDoneSets == 1) "isn't" else "aren't"
            add("${plural(summary.notDoneSets, "set")} $verb ticked and will be saved as not done.")
        }
        if (summary.emptyExercises > 0) {
            add("${plural(summary.emptyExercises, "exercise")} with no sets will be removed.")
        }
    }
}