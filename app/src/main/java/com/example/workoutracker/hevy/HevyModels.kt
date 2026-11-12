package com.example.workoutracker.hevy

/**
 * Hevy's data, as read from its CSV export, before it touches our database.
 * The parser produces these; the sync (Days 23-24) compares and saves them.
 * Keeping this in-between model means the database code never sees raw CSV text.
 */
data class HevyWorkout(
    val title: String,
    val startedAt: Long,          // epoch millis; Hevy's times have no zone, so the phone's is used
    val endedAt: Long,
    val description: String?,
    val exercises: List<HevyExercise>
)

/** One exercise block in a workout, in the order Hevy lists them. */
data class HevyExercise(
    val title: String,
    val notes: String?,
    val sets: List<HevySet>
)

data class HevySet(
    val index: Int,
    val type: String,             // "normal", "warmup", "failure", "dropset"
    val weightKg: Double?,
    val reps: Int?,
    val distanceMeters: Double?,
    val durationSeconds: Int?,
    val rpe: Double?
)

/** A row that couldn't be read, with its line number in the file (the header is line 1). */
data class RowProblem(val line: Int, val message: String)

/** Everything the parser found: the workouts it could read, and the rows it had to skip. */
data class HevyParseResult(
    val workouts: List<HevyWorkout>,
    val problems: List<RowProblem>
)

/** Thrown when the file isn't a Hevy workout export at all (e.g. a required column is missing). */
class HevyFormatException(message: String) : Exception(message)