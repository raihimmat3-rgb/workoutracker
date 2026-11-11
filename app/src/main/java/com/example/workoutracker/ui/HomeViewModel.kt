package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.workoutracker.data.TemplateWithExercises
import com.example.workoutracker.data.WorkoutExerciseEntity
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.WorkoutEntity
import com.example.workoutracker.data.WorkoutSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

private const val WEEKS_SHOWN = 8

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)
    private val dao = db.workoutDao()
    private val templateDao = db.templateDao()

    /** The workout in progress, or null. */
    val activeWorkout: StateFlow<WorkoutEntity?> =
        dao.observeActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Reuses the History query: it already has each finished workout's duration, sets and volume.
    private val history = dao.observeHistorySummaries()

    /** The most recent finished workout, for the "Last workout" card. */
    val lastWorkout: StateFlow<WorkoutSummary?> =
        history.map { it.firstOrNull() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The last 8 weeks' totals, oldest first. The last entry is this week. */
    val weeks: StateFlow<List<WeekTotals>> =
        history.map { weeklyTotals(it, ZoneId.systemDefault(), System.currentTimeMillis(), WEEKS_SHOWN) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Saved routines, A to Z, each with its exercises in order. */
    val templates: StateFlow<List<TemplateWithExercises>> =
        templateDao.observeAll()
            .map { list ->
                list.map { t -> t.copy(exercises = t.exercises.sortedBy { it.templateExercise.position }) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Starts a workout from a routine: its exercises in order, each with the planned number of
     * sets pre-filled from last time (not ticked). If a workout is already in progress, it
     * opens that one instead, like the Start button.
     */
    fun startFromTemplate(templateId: Long, onReady: (Long) -> Unit) {
        viewModelScope.launch {
            dao.getActive()?.let { onReady(it.id); return@launch }
            val template = templateDao.getTemplate(templateId) ?: return@launch
            val workoutId = db.withTransaction {
                val id = dao.insertWorkout(
                    WorkoutEntity(title = template.template.name, startedAt = System.currentTimeMillis())
                )
                if (id == -1L) return@withTransaction -1L
                template.exercises
                    .sortedBy { it.templateExercise.position }
                    .forEachIndexed { position, item ->
                        val weId = dao.insertWorkoutExercise(
                            WorkoutExerciseEntity(
                                workoutId = id,
                                exerciseId = item.exercise.id,
                                position = position
                            )
                        )
                        val previous = dao.getPreviousSets(item.exercise.id, id)
                        dao.insertSets(plannedSets(item.templateExercise.setCount, previous, weId))
                    }
                id
            }
            if (workoutId != -1L) onReady(workoutId)
        }
    }

    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch { templateDao.deleteTemplate(templateId) }
    }

    /**
     * Starts a new workout and reports its id, unless one is already in progress,
     * in which case it reports that one instead (so two taps never create two workouts).
     */
    fun startWorkout(onReady: (Long) -> Unit) {
        viewModelScope.launch {
            val existing = dao.getActive()
            if (existing != null) {
                onReady(existing.id)
                return@launch
            }
            val id = dao.insertWorkout(
                WorkoutEntity(
                    title = defaultWorkoutTitle(LocalTime.now().hour),
                    startedAt = System.currentTimeMillis()
                )
            )
            if (id != -1L) onReady(id)
        }
    }
}