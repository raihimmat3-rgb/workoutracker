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
import com.example.workoutracker.coach.CoachClient
import com.example.workoutracker.coach.CoachException
import com.example.workoutracker.coach.CoachRepository
import com.example.workoutracker.coach.CoachReviewDto
import com.example.workoutracker.hevy.fingerprint
import com.example.workoutracker.hevy.toContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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

    // ---- Coach (Day 32) ----

    private val coach = CoachRepository(db, CoachClient.create())
    private val coachLoading = MutableStateFlow(false)
    private val coachError = MutableStateFlow<String?>(null)

    /**
     * What the Coach card shows, worked out from: the saved review (if any), the workout
     * (to spot edits since the review), and whether a request is running or just failed.
     */
    val coachState: StateFlow<CoachUiState> =
        combine(coach.observeSaved(workoutId), details, coachLoading, coachError) { saved, current, loading, error ->
            val ready = saved?.let {
                runCatching { coach.decode(it) }.getOrNull()?.let { review ->
                    CoachUiState.Ready(
                        review = review,
                        createdAt = it.createdAt,
                        outdated = current != null && current.toContent().fingerprint() != it.workoutFingerprint
                    )
                }
            }
            when {
                !CoachClient.isConfigured && ready == null -> CoachUiState.NotSetUp
                loading -> CoachUiState.Loading
                error != null -> CoachUiState.Failed(error, ready)
                ready != null -> ready
                else -> CoachUiState.Idle
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachUiState.Idle)

    /** Asks for a (new) review. Each call uses one of today's coach requests. */
    fun askCoach() {
        if (coachLoading.value) return // ignore a second tap while one is running
        coachLoading.value = true
        coachError.value = null
        viewModelScope.launch {
            try {
                coach.requestReview(workoutId) // saved to the database; coachState updates by itself
            } catch (e: CoachException) {
                coachError.value = e.message
            } catch (e: Exception) {
                coachError.value = "Something went wrong: ${e.message}"
            } finally {
                coachLoading.value = false
            }
        }
    }

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

/** Every state the Coach card can be in. */
sealed interface CoachUiState {
    /** No server address in local.properties. */
    data object NotSetUp : CoachUiState
    data object Idle : CoachUiState
    data object Loading : CoachUiState
    data class Ready(val review: CoachReviewDto, val createdAt: Long, val outdated: Boolean) : CoachUiState
    /** [previous] is the last saved review, still shown under the error if there is one. */
    data class Failed(val message: String, val previous: Ready?) : CoachUiState
}