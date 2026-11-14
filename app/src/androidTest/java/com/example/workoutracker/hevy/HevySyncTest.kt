package com.example.workoutracker.hevy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.WorkoutEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId

/**
 * Runs the whole sync (parse -> prepare -> apply) against a throwaway database on your phone.
 * Goes in app/src/androidTest/java/com/example/workoutracker/hevy/.
 */
@RunWith(AndroidJUnit4::class)
class HevySyncTest {

    private lateinit var db: AppDatabase
    private lateinit var sync: HevySync
    private val parser = HevyCsvParser(ZoneId.of("Europe/London"))

    private val header =
        "\"title\",\"start_time\",\"end_time\",\"description\",\"exercise_title\",\"superset_id\"," +
                "\"exercise_notes\",\"set_index\",\"set_type\",\"weight_kg\",\"reps\",\"distance_miles\"," +
                "\"duration_seconds\",\"rpe\""
    private val pushRows = listOf(
        "\"push\",\"16 Oct 2026, 22:07\",\"16 Oct 2026, 23:37\",\"\",\"Bench Press (Barbell)\",,\"\",0,\"normal\",100,5,,,",
        "\"push\",\"16 Oct 2026, 22:07\",\"16 Oct 2026, 23:37\",\"\",\"Bench Press (Barbell)\",,\"\",1,\"normal\",97.5,6,,,"
    )
    private val legsRows = listOf(
        "\"legs\",\"9 Sep 2026, 07:05\",\"9 Sep 2026, 08:10\",\"\",\"Squat (Barbell)\",,\"\",0,\"normal\",140,3,,,"
    )
    private fun file(vararg groups: List<String>) =
        parser.parse((listOf(header) + groups.toList().flatten()).joinToString("\n")).workouts

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        sync = HevySync(db)
    }

    @After
    fun closeDb() = db.close()

    private suspend fun syncAll(file: List<HevyWorkout>, resolutions: Map<Long, Resolution> = emptyMap()) =
        sync.apply(sync.prepare(file), resolutions)

    private suspend fun benchWorkout() =
        db.workoutDao().getImportedWithDetails().single { it.workout.title == "push" }

    @Test
    fun firstSyncImportsEverything_andASecondSyncChangesNothing() = runBlocking {
        val result = syncAll(file(pushRows, legsRows))
        assertEquals(2, result.added)

        val push = benchWorkout()
        assertEquals("hevy", push.workout.source)
        assertEquals(listOf(5, 6), push.exercises.single().sets.sortedBy { it.setIndex }.map { it.reps })
        assertEquals(true, push.exercises.single().sets.all { it.isDone })

        // The key check: what we stored fingerprints the same as the file.
        val again = sync.prepare(file(pushRows, legsRows)).plan
        assertEquals(2, again.unchanged)
        assertEquals(false, again.hasChanges)
    }

    @Test
    fun deletedInHevy_isDeletedHere() = runBlocking {
        syncAll(file(pushRows, legsRows))
        val result = syncAll(file(pushRows))
        assertEquals(1, result.deleted)
        assertEquals(listOf("push"), db.workoutDao().getImportedWithDetails().map { it.workout.title })
    }

    @Test
    fun editedHere_keepMine_keepsItAndDoesNotAskAgain() = runBlocking {
        syncAll(file(pushRows))
        val set = benchWorkout().exercises.single().sets.minBy { it.setIndex }
        db.workoutDao().updateSetValues(set.id, 100.0, 8) // your edit: 5 reps -> 8

        val prepared = sync.prepare(file(pushRows))
        val conflict = prepared.plan.conflicts.single()
        assertEquals(ConflictKind.EditedHere, conflict.kind)

        sync.apply(prepared, mapOf(conflict.localId to Resolution.KeepMine))
        assertEquals(8, benchWorkout().exercises.single().sets.minBy { it.setIndex }.reps)
        assertEquals(false, sync.prepare(file(pushRows)).plan.hasChanges)
    }

    @Test
    fun editedHere_useHevy_revertsIt() = runBlocking {
        syncAll(file(pushRows))
        val set = benchWorkout().exercises.single().sets.minBy { it.setIndex }
        db.workoutDao().updateSetValues(set.id, 100.0, 8)

        val prepared = sync.prepare(file(pushRows))
        sync.apply(prepared, mapOf(prepared.plan.conflicts.single().localId to Resolution.UseHevy))

        assertEquals(5, benchWorkout().exercises.single().sets.minBy { it.setIndex }.reps)
        assertEquals(false, sync.prepare(file(pushRows)).plan.hasChanges)
    }

    @Test
    fun undecidedConflicts_areLeftAlone() = runBlocking {
        syncAll(file(pushRows))
        val set = benchWorkout().exercises.single().sets.minBy { it.setIndex }
        db.workoutDao().updateSetValues(set.id, 100.0, 8)

        val result = syncAll(file(pushRows)) // no answers given
        assertEquals(1, result.undecided)
        assertEquals(8, benchWorkout().exercises.single().sets.minBy { it.setIndex }.reps)
    }

    @Test
    fun deletedInHevyButEdited_keepMine_makesItAnOrdinaryWorkout() = runBlocking {
        syncAll(file(pushRows))
        val push = benchWorkout()
        db.workoutDao().updateSetValues(push.exercises.single().sets.first().id, 100.0, 9)

        val prepared = sync.prepare(file(legsRows)) // push is gone from Hevy
        val conflict = prepared.plan.conflicts.single()
        assertEquals(ConflictKind.DeletedInHevy, conflict.kind)
        sync.apply(prepared, mapOf(conflict.localId to Resolution.KeepMine))

        val kept = db.workoutDao().getById(push.workout.id)!!
        assertEquals("manual", kept.source)
        assertNull(kept.hevyKey)
    }

    @Test
    fun workoutsMadeInTheApp_areNeverTouched_andBlockClashes() = runBlocking {
        val legsStart = file(legsRows).single().startedAt
        val mineId = db.workoutDao().insertWorkout(WorkoutEntity(title = "mine", startedAt = legsStart, endedAt = legsStart + 1))

        val result = syncAll(file(pushRows, legsRows))

        assertEquals(1, result.added)    // push
        assertEquals(1, result.skipped)  // legs: same minute as "mine"
        assertEquals("mine", db.workoutDao().getById(mineId)!!.title)
    }

    @Test
    fun exercisesAreMatchedToTheLibraryIgnoringCase() = runBlocking {
        db.exerciseDao().insert(com.example.workoutracker.data.ExerciseEntity(name = "bench press (barbell)"))
        syncAll(file(pushRows))
        assertEquals("bench press (barbell)", benchWorkout().exercises.single().exercise.name)
        assertEquals(false, sync.prepare(file(pushRows)).plan.hasChanges)
    }
}