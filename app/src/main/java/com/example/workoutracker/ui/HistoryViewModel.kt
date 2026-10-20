package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.WorkoutEntity
import com.example.workoutracker.data.seedSampleData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the screen's data so it survives rotation, and turns the database Flow into a StateFlow
 * the UI can observe. The UI only reads `workouts` and calls the functions below.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)

    val workouts: StateFlow<List<WorkoutEntity>> =
        db.workoutDao().observeHistory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun addSampleData() {
        viewModelScope.launch { seedSampleData(db) }
    }

    fun delete(workoutId: Long) {
        viewModelScope.launch { db.workoutDao().deleteWorkout(workoutId) }
    }
}