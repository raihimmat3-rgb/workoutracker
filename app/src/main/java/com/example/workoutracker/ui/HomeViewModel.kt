package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.WorkoutEntity
import java.util.Calendar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)

    val activeWorkout: StateFlow<WorkoutEntity?> =
        db.workoutDao().observeActive()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    fun startWorkout(onOpenWorkout: (Long) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val workout = WorkoutEntity(
                title = defaultWorkoutTitle(hour),
                startedAt = now
            )
            val id = db.workoutDao().insertWorkout(workout)
            onOpenWorkout(id)
        }
    }
}
