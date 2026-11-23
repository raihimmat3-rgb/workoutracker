package com.example.workoutracker.coach

import com.example.workoutracker.data.WorkoutSetEntity
import com.example.workoutracker.data.WorkoutWithDetails
import com.example.workoutracker.ui.formatWeight
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/*
 * The app's half of "the app does the maths, the AI explains it". Everything here is plain
 * Kotlin with no network or database, so it's fully unit-tested: the numbers the coach is
 * given are guaranteed to be right.
 */

/** Just the numbers of one set. */
data class SetValues(val weightKg: Double?, val reps: Int?)

fun WorkoutSetEntity.values() = SetValues(weightKg, reps)

/** Last time this exercise was done: when, and its ticked sets in order. */
data class PreviousData(val startedAt: Long, val sets: List<WorkoutSetEntity>)

/** The server accepts at most this many "changes" lines per exercise. */
const val MAX_CHANGE_LINES = 10

/**
 * Plain-English facts comparing today's ticked sets with last time's, plus any fade within today.
 * Sets are matched in order (1st with 1st...). Examples:
 *   "Top weight up from 97.5 kg to 100 kg"
 *   "4 sets today vs 3 last time"
 *   "Set 3: 3 reps today vs 5 last time at 100 kg"
 *   "Set 2: 100 kg × 5 today vs 97.5 kg × 5 last time"
 *   "Within today: reps fell from 8 on set 1 to 5 on set 3 at 60 kg"
 */
fun describeChanges(today: List<SetValues>, previous: List<SetValues>?): List<String> {
    val lines = mutableListOf<String>()

    if (previous.isNullOrEmpty()) {
        lines += "First time logging this exercise"
    } else {
        val topToday = today.mapNotNull { it.weightKg }.maxOrNull()
        val topBefore = previous.mapNotNull { it.weightKg }.maxOrNull()
        if (topToday != null && topBefore != null && topToday != topBefore) {
            val direction = if (topToday > topBefore) "up" else "down"
            lines += "Top weight $direction from ${kg(topBefore)} to ${kg(topToday)}"
        }
        if (today.size != previous.size) {
            lines += "${today.size} sets today vs ${previous.size} last time"
        }
        for (i in 0 until minOf(today.size, previous.size)) {
            val now = today[i]
            val then = previous[i]
            if (now.weightKg == then.weightKg) {
                if (now.reps != null && then.reps != null && now.reps != then.reps) {
                    val at = now.weightKg?.let { " at ${kg(it)}" } ?: ""
                    lines += "Set ${i + 1}: ${now.reps} reps today vs ${then.reps} last time$at"
                }
            } else {
                lines += "Set ${i + 1}: ${set(now)} today vs ${set(then)} last time"
            }
        }
    }

    // A fade within today's session at the same weight: a common, explainable rep drop.
    val first = today.firstOrNull()
    if (first?.reps != null) {
        val worstIndex = today.indices.drop(1)
            .filter { today[it].weightKg == first.weightKg && today[it].reps != null }
            .minByOrNull { today[it].reps!! }
        if (worstIndex != null && first.reps - today[worstIndex].reps!! >= 2) {
            val at = first.weightKg?.let { " at ${kg(it)}" } ?: ""
            lines += "Within today: reps fell from ${first.reps} on set 1 to ${today[worstIndex].reps} on set ${worstIndex + 1}$at"
        }
    }
    return lines.take(MAX_CHANGE_LINES)
}

private fun kg(value: Double) = "${formatWeight(value)} kg"

private fun set(s: SetValues): String = when {
    s.weightKg != null && s.reps != null -> "${kg(s.weightKg)} × ${s.reps}"
    s.weightKg != null -> kg(s.weightKg)
    s.reps != null -> "${s.reps} reps"
    else -> "an empty set"
}

/**
 * Builds the review request for a finished workout. Returns null if nothing was ticked
 * (there's nothing to review, so no request is spent).
 * Only ticked sets are sent, and lists are capped at the server's limits.
 */
fun buildReviewRequest(
    details: WorkoutWithDetails,
    previous: Map<Long, PreviousData?>,          // exercise id -> last time
    daysSinceLastWorkout: Int?,
    workoutsThisWeek: Int?,
    zone: ZoneId
): ReviewRequestDto? {
    val workout = details.workout
    val exercises = details.exercises
        .sortedBy { it.workoutExercise.position }
        .mapNotNull { item ->
            val done = item.sets.filter { it.isDone }.sortedBy { it.setIndex }.take(20)
            if (done.isEmpty()) return@mapNotNull null
            val before = previous[item.exercise.id]
            ExerciseDataDto(
                name = item.exercise.name,
                muscleGroup = item.exercise.muscleGroup,
                sets = done.map { it.toDto() },
                previous = before?.let {
                    PreviousSessionDto(
                        daysAgo = daysBetween(it.startedAt, workout.startedAt, zone),
                        sets = it.sets.take(20).map { s -> s.toDto() }
                    )
                },
                changes = describeChanges(done.map { it.values() }, before?.sets?.map { it.values() })
            )
        }
        .take(20)
    if (exercises.isEmpty()) return null

    val date = Instant.ofEpochMilli(workout.startedAt).atZone(zone).toLocalDate()
    val minutes = workout.endedAt?.let { ((it - workout.startedAt) / 60_000).toInt() } ?: 0
    return ReviewRequestDto(
        workout = WorkoutInfoDto(title = workout.title.take(80), date = date.toString(), durationMinutes = minutes.coerceAtLeast(0)),
        exercises = exercises,
        context = TrainingContextDto(daysSinceLastWorkout, workoutsThisWeek)
    )
}

/** Whole calendar days between two moments in [zone] (yesterday evening -> this morning = 1). */
fun daysBetween(earlier: Long, later: Long, zone: ZoneId): Int =
    ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(earlier).atZone(zone).toLocalDate(),
        Instant.ofEpochMilli(later).atZone(zone).toLocalDate()
    ).toInt()

private fun WorkoutSetEntity.toDto() = SetDataDto(weightKg = weightKg, reps = reps, type = setType)