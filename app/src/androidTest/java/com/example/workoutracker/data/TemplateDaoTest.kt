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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for routines. A new file, next to WorkoutDaoTest in androidTest. */
@RunWith(AndroidJUnit4::class)
class TemplateDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var templateDao: TemplateDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        templateDao = db.templateDao()
    }

    @After
    fun closeDb() = db.close()

    private suspend fun exercise(name: String) = db.exerciseDao().insert(ExerciseEntity(name = name))

    private suspend fun template(name: String, vararg exerciseIds: Long): Long {
        val id = templateDao.insertTemplate(TemplateEntity(name = name, createdAt = 0))
        templateDao.insertTemplateExercises(
            exerciseIds.mapIndexed { i, ex -> TemplateExerciseEntity(templateId = id, exerciseId = ex, position = i, setCount = 3) }
        )
        return id
    }

    private fun countRows(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
            it.moveToFirst()
            it.getInt(0)
        }

    @Test
    fun templates_comeBackAToZWithTheirExercises() = runBlocking {
        val bench = exercise("Bench")
        val fly = exercise("Fly")
        template("push day", bench, fly)
        template("Legs", bench)

        val all = templateDao.observeAll().first()

        assertEquals(listOf("Legs", "push day"), all.map { it.template.name }) // ignores case
        val push = all[1]
        assertEquals(
            listOf("Bench", "Fly"),
            push.exercises.sortedBy { it.templateExercise.position }.map { it.exercise.name }
        )
    }

    @Test
    fun deletingTemplate_removesItsExercisesButNotTheLibrary() = runBlocking {
        val bench = exercise("Bench")
        val id = template("Push", bench)

        assertEquals(1, templateDao.deleteTemplate(id))

        assertEquals(0, countRows("template_exercise"))
        assertEquals(1, countRows("exercise"))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun exerciseUsedInATemplate_cantBeDeleted() {
        runBlocking {
            val bench = exercise("Bench")
            template("Push", bench)
            db.openHelper.writableDatabase.execSQL("DELETE FROM exercise WHERE id = $bench")
        }
    }
}