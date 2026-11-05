package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.WorkoutWithDetails
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A finished workout, read-only for now (editing is Day 16). */
class WorkoutDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])
    private val dao = AppDatabase.get(application).workoutDao()

    /** The workout with exercises in position order and sets in setIndex order. */
    val details: StateFlow<WorkoutWithDetails?> =
        dao.observeDetails(workoutId)
            .map { details ->
                details?.copy(
                    exercises = details.exercises
                        .sortedBy { it.workoutExercise.position }
                        .map { it.copy(sets = it.sets.sortedBy { set -> set.setIndex }) }
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Deletes the workout and everything in it (cascade), then calls [onDone]. */
    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            dao.deleteWorkout(workoutId)
            onDone()
        }
    }
}