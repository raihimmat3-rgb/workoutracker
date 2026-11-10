package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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
    val sessions: List<ExerciseSession>
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
            workoutDao.observeExerciseSessions(exerciseId)
        ) { exercise, bests, stats, sessions ->
            exercise?.let { ExerciseDetailState(it, bests, stats, sessions) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}