package com.example.workoutracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class
    ],
    version = 2,
    exportSchema = true   // writes a schema JSON you can commit; useful for migrations later
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}

/**
 * Version 1 -> 2: adds workout_set.isDone.
 * Your existing data is kept. Sets that were already saved in FINISHED workouts with reps
 * are marked done, since under the old rules that's exactly what "logged" meant.
 * Room runs this once, the first time the new version of the app opens the database.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_set ADD COLUMN isDone INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            UPDATE workout_set SET isDone = 1
            WHERE reps IS NOT NULL
              AND workoutExerciseId IN (
                  SELECT we.id FROM workout_exercise we
                  JOIN workout w ON w.id = we.workoutId
                  WHERE w.endedAt IS NOT NULL
              )
            """.trimIndent()
        )
    }
}