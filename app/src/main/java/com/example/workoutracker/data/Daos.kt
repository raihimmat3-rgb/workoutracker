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

    @Query("SELECT * FROM exercise WHERE id = :id")
    fun observeById(id: Long): Flow<ExerciseEntity?>

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

    /** The next free position in a workout: 0 for the first exercise, then 1, 2, ... */
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM workout_exercise WHERE workoutId = :workoutId")
    suspend fun nextPosition(workoutId: Long): Int

    /** This workout's exercises, in order. Used inside transactions (not observed). */
    @Query("SELECT * FROM workout_exercise WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getWorkoutExercises(workoutId: Long): List<WorkoutExerciseEntity>

    @Query("UPDATE workout_exercise SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int): Int

    /** Removes every exercise (and, by cascade, every set) from a workout. Used to undo edits. */
    @Query("DELETE FROM workout_exercise WHERE workoutId = :workoutId")
    suspend fun deleteExercisesInWorkout(workoutId: Long): Int

    // ---- Personal bests (Day 17) ----
    // All of these only look at TICKED sets in FINISHED workouts. Ties go to the earliest
    // workout, because that's when the record was first set.

    /** Heaviest weight lifted; more reps wins a tie on weight. */
    @Query(
        """
        SELECT s.weightKg, s.reps, w.startedAt, w.id AS workoutId
        FROM workout_set s
        JOIN workout_exercise we ON we.id = s.workoutExerciseId
        JOIN workout w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.isDone = 1 AND w.endedAt IS NOT NULL
          AND s.weightKg IS NOT NULL
        ORDER BY s.weightKg DESC, s.reps DESC, w.startedAt ASC
        LIMIT 1
        """
    )
    fun observeHeaviestSet(exerciseId: Long): Flow<SetRecord?>

    /**
     * Best estimated one-rep max (Epley: weight x (1 + reps / 30)). A single rep counts as
     * exactly its weight. Must match estimateOneRepMax() in Kotlin.
     */
    @Query(
        """
        SELECT s.weightKg, s.reps, w.startedAt, w.id AS workoutId
        FROM workout_set s
        JOIN workout_exercise we ON we.id = s.workoutExerciseId
        JOIN workout w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.isDone = 1 AND w.endedAt IS NOT NULL
          AND s.weightKg IS NOT NULL AND s.reps IS NOT NULL AND s.reps > 0
        ORDER BY CASE WHEN s.reps = 1 THEN s.weightKg ELSE s.weightKg * (1 + s.reps / 30.0) END DESC,
                 w.startedAt ASC
        LIMIT 1
        """
    )
    fun observeBestOneRepMaxSet(exerciseId: Long): Flow<SetRecord?>

    /** Most reps in one set (useful for bodyweight); heavier wins a tie on reps. */
    @Query(
        """
        SELECT s.weightKg, s.reps, w.startedAt, w.id AS workoutId
        FROM workout_set s
        JOIN workout_exercise we ON we.id = s.workoutExerciseId
        JOIN workout w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.isDone = 1 AND w.endedAt IS NOT NULL
          AND s.reps IS NOT NULL
        ORDER BY s.reps DESC, s.weightKg DESC, w.startedAt ASC
        LIMIT 1
        """
    )
    fun observeMostRepsSet(exerciseId: Long): Flow<SetRecord?>

    /** Biggest single-set volume (weight x reps). */
    @Query(
        """
        SELECT s.weightKg, s.reps, w.startedAt, w.id AS workoutId
        FROM workout_set s
        JOIN workout_exercise we ON we.id = s.workoutExerciseId
        JOIN workout w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.isDone = 1 AND w.endedAt IS NOT NULL
          AND s.weightKg IS NOT NULL AND s.reps IS NOT NULL
        ORDER BY s.weightKg * s.reps DESC, w.startedAt ASC
        LIMIT 1
        """
    )
    fun observeBestVolumeSet(exerciseId: Long): Flow<SetRecord?>

    /** How many workouts the exercise was done in, and when last. Always returns one row. */
    @Query(
        """
        SELECT COUNT(DISTINCT w.id) AS workoutCount, MAX(w.startedAt) AS lastPerformedAt
        FROM workout_set s
        JOIN workout_exercise we ON we.id = s.workoutExerciseId
        JOIN workout w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.isDone = 1 AND w.endedAt IS NOT NULL
        """
    )
    fun observeExerciseStats(exerciseId: Long): Flow<ExerciseStats>

    /** Removes one exercise from a workout; its sets go with it (cascade). Returns rows deleted. */
    @Query("DELETE FROM workout_exercise WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: Long): Int

    @Insert
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    /** The highest-numbered set of one exercise in a workout, or null if it has none yet. */
    @Query("SELECT * FROM workout_set WHERE workoutExerciseId = :workoutExerciseId ORDER BY setIndex DESC LIMIT 1")
    suspend fun getLastSet(workoutExerciseId: Long): WorkoutSetEntity?

    /**
     * The sets you DID (ticked) for [exerciseId] the last time you trained it: taken from the most
     * recent FINISHED workout where it has at least one ticked set (other than [currentWorkoutId]).
     * If that workout had the exercise twice, the first occurrence is used. Empty if no history.
     */
    @Query(
        """
        SELECT s.* FROM workout_set s
        WHERE s.workoutExerciseId = (
            SELECT we.id FROM workout_exercise we
            JOIN workout w ON w.id = we.workoutId
            WHERE we.exerciseId = :exerciseId
              AND w.endedAt IS NOT NULL
              AND w.id != :currentWorkoutId
              AND EXISTS (SELECT 1 FROM workout_set d WHERE d.workoutExerciseId = we.id AND d.isDone = 1)
            ORDER BY w.startedAt DESC, we.position ASC
            LIMIT 1
        )
          AND s.isDone = 1
        ORDER BY s.setIndex
        """
    )
    suspend fun getPreviousSets(exerciseId: Long, currentWorkoutId: Long): List<WorkoutSetEntity>

    /**
     * Changes the weight and reps of one set. Clearing the reps also unticks the set,
     * because a set can't be done without reps. Returns rows updated.
     */
    @Query(
        """
        UPDATE workout_set
        SET weightKg = :weightKg,
            reps = :reps,
            isDone = CASE WHEN :reps IS NULL THEN 0 ELSE isDone END
        WHERE id = :id
        """
    )
    suspend fun updateSetValues(id: Long, weightKg: Double?, reps: Int?): Int

    /** Ticks or unticks a set. Ticking is refused (0 rows) if the set has no reps. */
    @Query("UPDATE workout_set SET isDone = :isDone WHERE id = :id AND (:isDone = 0 OR reps IS NOT NULL)")
    suspend fun setDone(id: Long, isDone: Boolean): Int

    @Query("DELETE FROM workout_set WHERE id = :id")
    suspend fun deleteSet(id: Long): Int

    /** Deletes exercises in this workout that have no sets left. Returns how many. */
    @Query(
        """
        DELETE FROM workout_exercise
        WHERE workoutId = :workoutId
          AND id NOT IN (SELECT DISTINCT workoutExerciseId FROM workout_set)
        """
    )
    suspend fun deleteExercisesWithoutSets(workoutId: Long): Int

    /** Returns how many rows were updated (1 if it worked, 0 if no workout had that id). */
    @Update
    suspend fun updateWorkout(workout: WorkoutEntity): Int

    /** Cascades to the workout's exercises and sets. Returns how many workouts were deleted. */
    @Query("DELETE FROM workout WHERE id = :id")
    suspend fun deleteWorkout(id: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM workout WHERE startedAt = :startedAt)")
    suspend fun existsByStartedAt(startedAt: Long): Boolean

    @Query("SELECT * FROM workout WHERE id = :id")
    suspend fun getById(id: Long): WorkoutEntity?

    @Query("SELECT * FROM workout WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutEntity?>

    /** The workout currently in progress (endedAt is still null), if there is one. */
    @Query("SELECT * FROM workout WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workout WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(): WorkoutEntity?

    /**
     * Finished workouts with their totals, newest first, for the History list.
     * Only ticked (done) sets count towards sets and volume. weightKg * reps is NULL for
     * bodyweight sets, and SUM skips NULLs, so they add nothing to volume.
     */
    @Query(
        """
        SELECT w.id, w.title, w.startedAt, w.endedAt, w.source,
            (SELECT COUNT(*) FROM workout_exercise we WHERE we.workoutId = w.id) AS exerciseCount,
            (SELECT COUNT(*) FROM workout_set s
                JOIN workout_exercise we ON we.id = s.workoutExerciseId
                WHERE we.workoutId = w.id AND s.isDone = 1) AS doneSets,
            (SELECT COALESCE(SUM(s.weightKg * s.reps), 0) FROM workout_set s
                JOIN workout_exercise we ON we.id = s.workoutExerciseId
                WHERE we.workoutId = w.id AND s.isDone = 1) AS volumeKg
        FROM workout w
        WHERE w.endedAt IS NOT NULL
        ORDER BY w.startedAt DESC
        """
    )
    fun observeHistorySummaries(): Flow<List<WorkoutSummary>>

    /** Finished workouts only, newest first, for the history list. */
    @Query("SELECT * FROM workout WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<WorkoutEntity>>

    @Transaction
    @Query("SELECT * FROM workout WHERE id = :id")
    fun observeDetails(id: Long): Flow<WorkoutWithDetails?>

    /** For the "last workout" card on the home screen. */
    @Transaction
    @Query("SELECT * FROM workout WHERE endedAt IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeLastFinished(): Flow<WorkoutWithDetails?>
}