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
import com.example.workoutracker.data.WorkoutWithDetails
import com.example.workoutracker.data.WorkoutSetEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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
            .map { details ->
                details?.exercises.orEmpty()
                    .sortedBy { it.workoutExercise.position }
                    // Sets aren't promised in order either, so sort them by setIndex.
                    .map { it.copy(sets = it.sets.sortedBy { set -> set.setIndex }) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Exercise id -> its sets from the last finished workout that had it (the "Previous" column).
     * Only re-queried when the list of exercises changes, not on every keystroke,
     * because past workouts don't change while you're training.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val previousSets: StateFlow<Map<Long, List<WorkoutSetEntity>>> =
        exercises
            .map { list -> list.map { it.exercise.id }.distinct() }
            .distinctUntilChanged()
            .mapLatest { ids -> ids.associateWith { dao.getPreviousSets(it, workoutId) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

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

    /**
     * Adds a set to the end of an exercise. It copies the weight and reps of the previous set,
     * since most sets repeat the one before, so usually you only change one number.
     */
    fun addSet(workoutExerciseId: Long) {
        viewModelScope.launch {
            db.withTransaction {
                val last = dao.getLastSet(workoutExerciseId)
                dao.insertSet(
                    WorkoutSetEntity(
                        workoutExerciseId = workoutExerciseId,
                        setIndex = (last?.setIndex ?: -1) + 1,
                        weightKg = last?.weightKg,
                        reps = last?.reps
                    )
                )
            }
        }
    }

    /** Saves a set's weight and reps. Called as you type; null means the box is empty. */
    fun updateSet(setId: Long, weightKg: Double?, reps: Int?) {
        viewModelScope.launch { dao.updateSetValues(setId, weightKg, reps) }
    }

    /** Ticks or unticks a set. The database refuses to tick a set with no reps. */
    fun setDone(setId: Long, isDone: Boolean) {
        viewModelScope.launch { dao.setDone(setId, isDone) }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch { dao.deleteSet(setId) }
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
            // Tidy up and save in one transaction: either all of it happens or none of it.
            val rows = db.withTransaction {
                // Unticked sets are kept (saved as not done). Only exercises with no sets
                // at all are removed, matching summarizeForFinish.
                val exercisesRemoved = dao.deleteExercisesWithoutSets(workoutId)
                Log.d(TAG, "finish: removed $exercisesRemoved empty exercises")
                dao.updateWorkout(
                    current.copy(
                        title = title.trim().ifEmpty { current.title },
                        notes = notes.trim().ifEmpty { null },
                        endedAt = System.currentTimeMillis()
                    )
                )
            }
            Log.d(TAG, "finish: workout $workoutId, title='${title.trim()}', rows updated=$rows")
            if (rows == 1) onDone() else showError("Couldn't save the workout (rows updated: $rows)")
        }
    }

    private var saving = false

    /**
     * A copy of a finished workout exactly as it was when the editor opened, so "Discard changes"
     * can put it back. Kept in memory only: if Android kills the app mid-edit, the copy is lost
     * and the next one is taken from whatever was saved by then.
     */
    private var snapshot: WorkoutWithDetails? = null

    init {
        viewModelScope.launch {
            val original = dao.observeDetails(workoutId).first()
            // Only finished workouts are "edited"; a live workout has nothing to go back to.
            if (original?.workout?.endedAt != null) snapshot = original
        }
    }

    /** Puts the workout back to how it was when the editor opened, then calls [onDone]. */
    fun discardEdits(onDone: () -> Unit) {
        val original = snapshot
        if (original == null) {
            showError("Couldn't undo: the original wasn't loaded")
            return
        }
        if (saving) return
        saving = true
        viewModelScope.launch {
            db.withTransaction {
                // Wipe the edited version (sets go too, by cascade), then re-insert the copy.
                // The rows keep their original ids, so nothing else that points at them breaks.
                dao.deleteExercisesInWorkout(workoutId)
                original.exercises.forEach { item ->
                    dao.insertWorkoutExercise(item.workoutExercise)
                    dao.insertSets(item.sets)
                }
                dao.updateWorkout(original.workout)
            }
            Log.d(TAG, "discardEdits: workout $workoutId restored")
            onDone()
        }
    }

    /**
     * Saves changes to an already-finished workout (edit mode). Unlike [finish], it doesn't stamp
     * a new end time. If [newStartedAt] differs, the whole workout moves there and keeps its duration.
     */
    fun saveEdits(title: String, notes: String, newStartedAt: Long, onDone: () -> Unit) {
        if (saving) return // Save and the back button could both fire; only save once
        saving = true
        viewModelScope.launch {
            val current = dao.getById(workoutId)
            if (current == null) {
                saving = false
                showError("Couldn't find this workout (id $workoutId) to save it")
                return@launch
            }
            val (start, end) = shiftWorkoutTimes(
                startedAt = current.startedAt,
                endedAt = current.endedAt ?: current.startedAt,
                newStartedAt = newStartedAt
            )
            if (end > System.currentTimeMillis()) {
                saving = false
                showError("That start time would put the end of the workout in the future")
                return@launch
            }
            // startedAt is unique (it's how imports spot duplicates), so check before saving
            // rather than letting the database throw.
            if (start != current.startedAt && dao.existsByStartedAt(start)) {
                saving = false
                showError("Another workout already starts at exactly that time")
                return@launch
            }
            val rows = db.withTransaction {
                // Same tidy-up as finishing: exercises left with no sets are removed.
                dao.deleteExercisesWithoutSets(workoutId)
                dao.updateWorkout(
                    current.copy(
                        title = title.trim().ifEmpty { current.title },
                        notes = notes.trim().ifEmpty { null },
                        startedAt = start,
                        endedAt = end
                    )
                )
            }
            Log.d(TAG, "saveEdits: workout $workoutId, rows updated=$rows")
            if (rows == 1) {
                onDone()
            } else {
                saving = false
                showError("Couldn't save your changes (rows updated: $rows)")
            }
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