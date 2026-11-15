package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.ExerciseEntity
import com.example.workoutracker.data.ExerciseSession
import com.example.workoutracker.data.ExerciseStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Everything the exercise screen shows. */
data class ExerciseDetailState(
    val exercise: ExerciseEntity,
    val bests: PersonalBests,
    val stats: ExerciseStats,
    /** One entry per workout it was done in, oldest first, for the progress chart. */
    val sessions: List<ExerciseSession>,
    /** Used by any workout or routine: if so it can't be deleted. */
    val isUsed: Boolean
)

class ExerciseDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])
    private val db = AppDatabase.get(application)
    private val workoutDao = db.workoutDao()

    /**
     * Each record is its own small query. combine() waits until every query has answered,
     * then re-emits whenever any of them changes (e.g. after you finish a workout).
     */
    private val bests = combine(
        workoutDao.observeHeaviestSet(exerciseId),
        workoutDao.observeBestOneRepMaxSet(exerciseId),
        workoutDao.observeMostRepsSet(exerciseId),
        workoutDao.observeBestVolumeSet(exerciseId)
    ) { heaviest, oneRepMax, mostReps, volume -> PersonalBests(heaviest, oneRepMax, mostReps, volume) }

    /** null while loading, or if the exercise doesn't exist. */
    val state: StateFlow<ExerciseDetailState?> =
        combine(
            db.exerciseDao().observeById(exerciseId),
            bests,
            workoutDao.observeExerciseStats(exerciseId),
            workoutDao.observeExerciseSessions(exerciseId),
            db.exerciseDao().observeIsUsed(exerciseId)
        ) { exercise, bests, stats, sessions, isUsed ->
            exercise?.let { ExerciseDetailState(it, bests, stats, sessions, isUsed) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Saves the details you can change. Blank boxes are saved as "not set". */
    fun saveDetails(muscleGroup: String, equipment: String, instructions: String) {
        val current = state.value?.exercise ?: return
        viewModelScope.launch {
            db.exerciseDao().update(
                current.copy(
                    muscleGroup = muscleGroup.trim().ifEmpty { null },
                    equipment = equipment.trim().ifEmpty { null },
                    instructions = instructions.trim().ifEmpty { null }
                )
            )
        }
    }

    /** Only offered when nothing uses the exercise; the database would refuse otherwise anyway. */
    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            db.exerciseDao().delete(exerciseId)
            onDone()
        }
    }
}