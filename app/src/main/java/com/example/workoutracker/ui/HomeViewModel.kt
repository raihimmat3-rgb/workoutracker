package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    private val dao = AppDatabase.get(application).workoutDao()

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