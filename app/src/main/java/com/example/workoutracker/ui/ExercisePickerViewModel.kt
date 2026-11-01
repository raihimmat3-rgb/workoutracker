package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.ExerciseEntity
import com.example.workoutracker.data.WorkoutExerciseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Picks an exercise from the library and adds it to the end of a workout. */
class ExercisePickerViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])
    private val db = AppDatabase.get(application)
    private val exerciseDao = db.exerciseDao()
    private val workoutDao = db.workoutDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    @OptIn(ExperimentalCoroutinesApi::class)
    val exercises: StateFlow<List<ExerciseEntity>> =
        _query
            .flatMapLatest { q ->
                if (q.isBlank()) exerciseDao.observeAll() else exerciseDao.search(q.trim())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    private var adding = false

    /** Adds the exercise at the end of the workout, then calls [onDone]. */
    fun addExercise(exerciseId: Long, onDone: () -> Unit) {
        if (adding) return // ignore a second tap while the first one is still saving
        adding = true
        viewModelScope.launch {
            db.withTransaction { appendToWorkout(exerciseId) }
            onDone()
        }
    }

    /**
     * Creates a custom exercise called [name] and adds it to the workout, then calls [onDone].
     * Calls [onError] instead if the name isn't valid.
     */
    fun createAndAdd(name: String, onDone: () -> Unit, onError: (String) -> Unit) {
        validateExerciseName(name)?.let {
            onError(it)
            return
        }
        if (adding) return
        adding = true
        viewModelScope.launch {
            // Creating the exercise and adding it happen together: either both or neither.
            db.withTransaction {
                val trimmed = name.trim()
                val newId = exerciseDao.insert(ExerciseEntity(name = trimmed, isCustom = true))
                // -1 means the name already exists (names ignore case), so use that one instead.
                val exerciseId = if (newId != -1L) newId else exerciseDao.findByName(trimmed)!!.id
                appendToWorkout(exerciseId)
            }
            onDone()
        }
    }

    /** Must be called inside a transaction, so reading the next position and inserting can't interleave. */
    private suspend fun appendToWorkout(exerciseId: Long) {
        val position = workoutDao.nextPosition(workoutId)
        workoutDao.insertWorkoutExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, position = position)
        )
    }
}