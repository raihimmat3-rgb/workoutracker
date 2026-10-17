package com.example.workoutracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    /** Returns the new row id, or -1 if an exercise with that name already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Query("SELECT * FROM exercise WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ExerciseEntity?

    @Query("SELECT * FROM exercise ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun search(query: String): Flow<List<ExerciseEntity>>
}

@Dao
interface WorkoutDao {

    /** Returns the new row id, or -1 if a workout with that startedAt already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExerciseEntity): Long

    @Insert
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    /** Cascades to the workout's exercises and sets. */
    @Query("DELETE FROM workout WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM workout WHERE startedAt = :startedAt)")
    suspend fun existsByStartedAt(startedAt: Long): Boolean

    /** Newest first, for the history list. */
    @Query("SELECT * FROM workout ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<WorkoutEntity>>

    @Transaction
    @Query("SELECT * FROM workout WHERE id = :id")
    fun observeDetails(id: Long): Flow<WorkoutWithDetails?>

    /** For the "last workout" card on the home screen. */
    @Transaction
    @Query("SELECT * FROM workout WHERE endedAt IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeLastFinished(): Flow<WorkoutWithDetails?>
}