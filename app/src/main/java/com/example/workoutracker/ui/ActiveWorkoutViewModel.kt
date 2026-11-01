package com.example.workoutracker.ui

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.WorkoutEntity
import com.example.workoutracker.data.WorkoutExerciseWithSets
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "WorkoutTracker"

/**
 * The workout id arrives through navigation: the "workout/{workoutId}" route puts it in the
 * SavedStateHandle, so this ViewModel can read it without being passed it by hand.
 */
class ActiveWorkoutViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])
    private val db = AppDatabase.get(application)
    private val dao = db.workoutDao()

    val workout: StateFlow<WorkoutEntity?> =
        dao.observeById(workoutId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The exercises in this workout, in the order they were added. Room's @Relation doesn't
     * promise any order, so we sort by position ourselves (as the Day 5 test showed).
     */
    val exercises: StateFlow<List<WorkoutExerciseWithSets>> =
        dao.observeDetails(workoutId)
            .map { details -> details?.exercises.orEmpty().sortedBy { it.workoutExercise.position } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Moves an exercise one place up or down by swapping positions with its neighbour.
     * It re-reads the order inside a transaction rather than trusting the list on screen,
     * so a quick double-tap can't swap based on stale data.
     */
    fun moveExercise(workoutExerciseId: Long, up: Boolean) {
        viewModelScope.launch {
            db.withTransaction {
                val ordered = dao.getWorkoutExercises(workoutId)
                val index = ordered.indexOfFirst { it.id == workoutExerciseId }
                val target = neighbourIndex(index, ordered.size, up) ?: return@withTransaction
                val a = ordered[index]
                val b = ordered[target]
                dao.updatePosition(a.id, b.position)
                dao.updatePosition(b.id, a.position)
            }
        }
    }

    /** Removes one exercise (and its sets) from this workout. */
    fun removeExercise(workoutExerciseId: Long) {
        viewModelScope.launch {
            val rows = dao.deleteWorkoutExercise(workoutExerciseId)
            Log.d(TAG, "removeExercise: $workoutExerciseId, rows deleted=$rows")
        }
    }

    /**
     * Saves the title and notes, stamps the end time, then calls [onDone].
     * If the save fails, it shows a message and stays on the screen instead of navigating.
     */
    fun finish(title: String, notes: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val current = dao.getById(workoutId)
            if (current == null) {
                Log.w(TAG, "finish: no workout with id $workoutId")
                showError("Couldn't find this workout (id $workoutId) to save it")
                return@launch
            }
            val rows = dao.updateWorkout(
                current.copy(
                    title = title.trim().ifEmpty { current.title },
                    notes = notes.trim().ifEmpty { null },
                    endedAt = System.currentTimeMillis()
                )
            )
            Log.d(TAG, "finish: workout $workoutId, title='${title.trim()}', rows updated=$rows")
            if (rows == 1) onDone() else showError("Couldn't save the workout (rows updated: $rows)")
        }
    }

    /** Deletes the workout (and, via cascade, anything logged in it), then calls [onDone]. */
    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            val rows = dao.deleteWorkout(workoutId)
            Log.d(TAG, "discard: workout $workoutId, rows deleted=$rows")
            if (rows == 1) onDone() else showError("Couldn't discard the workout (rows deleted: $rows)")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
    }
}