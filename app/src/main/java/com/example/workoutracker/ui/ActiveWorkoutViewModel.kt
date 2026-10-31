package com.example.workoutracker.ui

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.WorkoutEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val dao = AppDatabase.get(application).workoutDao()

    val workout: StateFlow<WorkoutEntity?> =
        dao.observeById(workoutId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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