package com.example.workoutracker.hevy

import java.security.MessageDigest
import java.util.Locale

/**
 * A workout's content in one neutral shape, so a workout from the CSV and a workout from our
 * database can be compared. Deliberately leaves out the start time (that's the matching key)
 * and database ids (Hevy doesn't have ours).
 */
data class WorkoutContent(
    val title: String,
    val endedAt: Long?,
    val notes: String?,
    val exercises: List<ExerciseContent>
)

data class ExerciseContent(
    val name: String,
    val notes: String?,
    val sets: List<SetContent>
)

data class SetContent(
    val type: String,
    val weightKg: Double?,
    val reps: Int?,
    val distanceMeters: Double?,
    val durationSeconds: Int?,
    val rpe: Double?,
    val done: Boolean
)

/** Everything from Hevy counts as done: you only export workouts you finished. */
fun HevyWorkout.toContent(): WorkoutContent = WorkoutContent(
    title = title,
    endedAt = endedAt,
    notes = description,
    exercises = exercises.map { e ->
        ExerciseContent(
            name = e.title,
            notes = e.notes,
            sets = e.sets.map { s ->
                SetContent(s.type, s.weightKg, s.reps, s.distanceMeters, s.durationSeconds, s.rpe, done = true)
            }
        )
    }
)

/**
 * A short code that changes whenever anything that matters in the workout changes, and stays
 * the same otherwise. Saved at sync time, it lets the next sync tell "changed since last time"
 * apart from "the same" without keeping a full copy of every workout.
 *
 * Built from a canonical text form (fixed order, fixed number formats, names in lower case,
 * since the exercise library ignores case), then hashed with SHA-256.
 */
fun WorkoutContent.fingerprint(): String {
    val sep = '\u001F' // a control character that never appears in titles or notes
    val text = buildString {
        append(title.trim()).append(sep).append(endedAt).append(sep).append(notes?.trim().orEmpty()).append('\n')
        exercises.forEach { e ->
            append("E").append(sep).append(e.name.trim().lowercase(Locale.ROOT)).append(sep)
                .append(e.notes?.trim().orEmpty()).append('\n')
            e.sets.forEach { s ->
                append("S").append(sep).append(s.type.lowercase(Locale.ROOT)).append(sep)
                    .append(num(s.weightKg)).append(sep).append(s.reps ?: "").append(sep)
                    .append(num(s.distanceMeters)).append(sep).append(s.durationSeconds ?: "").append(sep)
                    .append(num(s.rpe)).append(sep).append(if (s.done) 1 else 0).append('\n')
            }
        }
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

/** 3 decimal places, so 27.2 and 27.200000000000003 (floating-point noise) count as the same. */
private fun num(value: Double?): String = value?.let { String.format(Locale.ROOT, "%.3f", it) } ?: ""