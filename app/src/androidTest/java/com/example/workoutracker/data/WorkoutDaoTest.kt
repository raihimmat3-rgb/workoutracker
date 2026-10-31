package com.example.workoutracker.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on your phone against a throwaway in-memory database,
 * so it never touches your real data.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutDao: WorkoutDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        exerciseDao = db.exerciseDao()
        workoutDao = db.workoutDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ---- helpers -------------------------------------------------------------

    private suspend fun newExercise(name: String): Long =
        exerciseDao.insert(ExerciseEntity(name = name))

    /** Inserts a workout with one exercise and the given (weightKg, reps) sets. */
    private suspend fun newWorkout(
        startedAt: Long,
        exerciseId: Long,
        sets: List<Pair<Double?, Int>>,
        title: String = "Test workout"
    ): Long {
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(title = title, startedAt = startedAt, endedAt = startedAt + 3_600_000)
        )
        val weId = workoutDao.insertWorkoutExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, position = 0)
        )
        workoutDao.insertSets(
            sets.mapIndexed { i, (kg, reps) ->
                WorkoutSetEntity(workoutExerciseId = weId, setIndex = i, weightKg = kg, reps = reps)
            }
        )
        return workoutId
    }

    private fun countRows(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
            it.moveToFirst()
            it.getInt(0)
        }

    // ---- tests ---------------------------------------------------------------

    @Test
    fun insertWorkout_thenReadBackWithDetails() = runBlocking {
        val bench = newExercise("Bench Press")
        val workoutId = newWorkout(1_000L, bench, listOf(60.0 to 8, 60.0 to 7))

        val details = workoutDao.observeDetails(workoutId).first()

        assertNotNull(details)
        assertEquals(1, details!!.exercises.size)
        val sets = details.exercises[0].sets.sortedBy { it.setIndex }
        assertEquals(2, sets.size)
        assertEquals(8, sets[0].reps)
        assertEquals(60.0, sets[0].weightKg!!, 0.0001)
        assertEquals("Bench Press", details.exercises[0].exercise.name)
    }

    @Test
    fun duplicateStartedAt_isIgnored() = runBlocking {
        val first = workoutDao.insertWorkout(WorkoutEntity(title = "A", startedAt = 5_000L))
        val second = workoutDao.insertWorkout(WorkoutEntity(title = "B", startedAt = 5_000L))

        assertEquals(true, first > 0)
        assertEquals(-1L, second)
        assertEquals(1, countRows("workout"))
    }

    @Test
    fun exerciseNames_areCaseInsensitiveUnique() = runBlocking {
        val first = newExercise("Bench Press")
        val second = newExercise("bench press")

        assertEquals(true, first > 0)
        assertEquals(-1L, second)
        assertEquals(1, countRows("exercise"))
    }

    @Test
    fun deletingWorkout_cascadesToExercisesAndSets() = runBlocking {
        val squat = newExercise("Squat")
        val workoutId = newWorkout(2_000L, squat, listOf(100.0 to 5, 100.0 to 5, 100.0 to 4))
        assertEquals(3, countRows("workout_set"))

        workoutDao.deleteWorkout(workoutId)

        assertEquals(0, countRows("workout"))
        assertEquals(0, countRows("workout_exercise"))
        assertEquals(0, countRows("workout_set"))
        // The library exercise itself must survive.
        assertEquals(1, countRows("exercise"))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun deletingExerciseWithHistory_isBlocked() {
        runBlocking {
            val deadlift = newExercise("Deadlift")
            newWorkout(3_000L, deadlift, listOf(140.0 to 3))
            db.openHelper.writableDatabase.execSQL("DELETE FROM exercise WHERE id = $deadlift")
        }
    }

    @Test
    fun history_isNewestFirst() = runBlocking {
        val curl = newExercise("Curl")
        newWorkout(1_000L, curl, listOf(20.0 to 10), title = "oldest")
        newWorkout(3_000L, curl, listOf(20.0 to 10), title = "newest")
        newWorkout(2_000L, curl, listOf(20.0 to 10), title = "middle")

        val titles = workoutDao.observeHistory().first().map { it.title }

        assertEquals(listOf("newest", "middle", "oldest"), titles)
    }

    @Test
    fun bodyweightSet_storesNullWeight() = runBlocking {
        val legRaise = newExercise("Hanging Leg Raise")
        val workoutId = newWorkout(4_000L, legRaise, listOf(null to 12))

        val set = workoutDao.observeDetails(workoutId).first()!!.exercises[0].sets[0]

        assertNull(set.weightKg)
        assertEquals(12, set.reps)
    }

    @Test
    fun twoExercisesInOneWorkout_keepTheirPositions() = runBlocking {
        val bench = newExercise("Bench Press")
        val fly = newExercise("Cable Fly")
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(title = "Chest", startedAt = 6_000L, endedAt = 9_600_000L)
        )

        // Insert in the "wrong" order on purpose, so the test proves position is what orders them.
        val flyWe = workoutDao.insertWorkoutExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = fly, position = 1)
        )
        val benchWe = workoutDao.insertWorkoutExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = bench, position = 0)
        )
        workoutDao.insertSet(WorkoutSetEntity(workoutExerciseId = benchWe, setIndex = 0, weightKg = 80.0, reps = 5))
        workoutDao.insertSet(WorkoutSetEntity(workoutExerciseId = flyWe, setIndex = 0, weightKg = 15.0, reps = 12))

        val ordered = workoutDao.observeDetails(workoutId).first()!!
            .exercises.sortedBy { it.workoutExercise.position }

        assertEquals(2, ordered.size)
        assertEquals(0, ordered[0].workoutExercise.position)
        assertEquals("Bench Press", ordered[0].exercise.name)
        assertEquals(80.0, ordered[0].sets[0].weightKg!!, 0.0001)
        assertEquals(1, ordered[1].workoutExercise.position)
        assertEquals("Cable Fly", ordered[1].exercise.name)
        assertEquals(12, ordered[1].sets[0].reps)
    }

    @Test
    fun history_excludesInProgressWorkouts() = runBlocking {
        val curl = newExercise("Curl")
        newWorkout(1_000L, curl, listOf(20.0 to 10), title = "done")
        // endedAt defaults to null, which means "still in progress".
        workoutDao.insertWorkout(WorkoutEntity(title = "in progress", startedAt = 2_000L))

        val titles = workoutDao.observeHistory().first().map { it.title }

        assertEquals(listOf("done"), titles)
    }

    @Test
    fun observeActive_returnsOnlyAnUnfinishedWorkout() = runBlocking {
        assertNull(workoutDao.observeActive().first())

        val id = workoutDao.insertWorkout(WorkoutEntity(title = "live", startedAt = 3_000L))
        assertEquals(id, workoutDao.observeActive().first()?.id)

        // Finishing it (setting endedAt) means there is no active workout any more.
        workoutDao.updateWorkout(workoutDao.getById(id)!!.copy(endedAt = 4_000L))
        assertNull(workoutDao.observeActive().first())
    }


    @Test
    fun search_matchesPartOfName() = runBlocking {
        newExercise("Lat Pulldown (Cable)")
        newExercise("Lying Leg Curl (Machine)")
        newExercise("Bench Press (Barbell)")

        val names = exerciseDao.search("curl").first().map { it.name }

        assertEquals(listOf("Lying Leg Curl (Machine)"), names)
    }
}