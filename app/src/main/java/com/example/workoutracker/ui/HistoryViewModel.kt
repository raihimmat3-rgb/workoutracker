package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.WorkoutSummary
import com.example.workoutracker.data.seedSampleData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)

    /** Finished workouts with their totals. Deleting now happens on the detail screen. */
    val workouts: StateFlow<List<WorkoutSummary>> =
        db.workoutDao().observeHistorySummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** TEMPORARY dev helper, removed once the Hevy import exists. */
    fun addSampleData() {
        viewModelScope.launch { seedSampleData(db) }
    }
}