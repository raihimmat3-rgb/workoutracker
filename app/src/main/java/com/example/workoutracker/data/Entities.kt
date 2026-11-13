package com.example.workoutracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** The exercise library. Names are unique and case-insensitive ("Bench Press" == "bench press"). */
@Entity(
    tableName = "exercise",
    indices = [Index(value = ["name"], unique = true)]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val muscleGroup: String? = null,
    val equipment: String? = null,
    val instructions: String? = null,
    val isCustom: Boolean = false
)

/**
 * One training session. Times are epoch milliseconds (UTC).
 * startedAt is unique: in your Hevy export every workout has its own start time,
 * so it works as the duplicate check when importing (titles are too messy to use).
 */
@Entity(
    tableName = "workout",
    indices = [
        Index(value = ["startedAt"], unique = true),
        // Each Hevy workout can only be imported once. NULL for workouts made in this app
        // (SQLite allows many NULLs in a unique index).
        Index(value = ["hevyKey"], unique = true)
    ]
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startedAt: Long,
    val endedAt: Long? = null,      // null while the workout is still in progress
    val notes: String? = null,
    val source: String = "manual",  // "manual", "sample" or "hevy"
    // ---- Hevy sync (database version 4). All null for workouts made in this app. ----
    /** Its start time in Hevy: how sync finds it in the file, even if you change the date here. */
    val hevyKey: Long? = null,
    /** Fingerprint of Hevy's version at the last sync. */
    val hevyFingerprint: String? = null,
    /** Fingerprint of this app's version at the last sync, so later edits here can be spotted. */
    val syncedFingerprint: String? = null
)

/** An exercise performed inside one workout (the join between workout and exercise). */
@Entity(
    tableName = "workout_exercise",
    foreignKeys = [
        // Deleting a workout deletes its exercises (and, below, their sets).
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        // RESTRICT: you can't delete a library exercise that still has history.
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("workoutId"), Index("exerciseId")]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,              // order within the workout
    val notes: String? = null
)

/** A single set. Weight is ALWAYS stored in kg; convert for display only. */
@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val setIndex: Int,
    val setType: String = SetType.NORMAL,
    val weightKg: Double? = null,   // null for bodyweight exercises
    val reps: Int? = null,          // null for timed exercises
    val rpe: Double? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    /** Ticked off during the workout. Added in database version 2 (see MIGRATION_1_2). */
    @ColumnInfo(defaultValue = "0") val isDone: Boolean = false
)

object SetType {
    const val NORMAL = "normal"
    const val WARMUP = "warmup"
    const val FAILURE = "failure"
    const val DROPSET = "dropset"
}