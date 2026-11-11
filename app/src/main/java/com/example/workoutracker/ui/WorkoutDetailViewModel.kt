package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.workoutracker.data.TemplateEntity
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
    private val db = AppDatabase.get(application)
    private val dao = db.workoutDao()

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

    /** Saves this workout's exercises (in order, with their set counts) as a new routine. */
    fun saveAsTemplate(name: String) {
        val current = details.value ?: return
        viewModelScope.launch {
            db.withTransaction {
                val templateId = db.templateDao().insertTemplate(
                    TemplateEntity(name = name.trim(), createdAt = System.currentTimeMillis())
                )
                db.templateDao().insertTemplateExercises(templateExercisesFrom(current, templateId))
            }
            Toast.makeText(getApplication(), "Saved \"${name.trim()}\" as a routine", Toast.LENGTH_SHORT).show()
        }
    }

    /** Deletes the workout and everything in it (cascade), then calls [onDone]. */
    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            dao.deleteWorkout(workoutId)
            onDone()
        }
    }
}