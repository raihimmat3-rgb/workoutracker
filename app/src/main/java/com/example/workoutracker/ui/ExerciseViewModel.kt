package com.example.workoutracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.ExerciseEntity
import com.example.workoutracker.data.seedHevyExercises
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.get(application).exerciseDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    /** Switches to a fresh database query every time the search text changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val exercises: StateFlow<List<ExerciseEntity>> =
        _query
            .flatMapLatest { q ->
                if (q.isBlank()) dao.observeAll() else dao.search(q.trim())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    /**
     * Saves a custom exercise. Calls [onResult] with an error message to show,
     * or null if it saved successfully.
     */
    fun addExercise(
        name: String,
        muscleGroup: String,
        equipment: String,
        instructions: String,
        onResult: (String?) -> Unit
    ) {
        validateExerciseName(name)?.let {
            onResult(it)
            return
        }
        viewModelScope.launch {
            val id = dao.insert(
                ExerciseEntity(
                    name = name.trim(),
                    muscleGroup = muscleGroup.trim().ifEmpty { null },
                    equipment = equipment.trim().ifEmpty { null },
                    instructions = instructions.trim().ifEmpty { null },
                    isCustom = true
                )
            )
            onResult(if (id == -1L) "An exercise with that name already exists" else null)
        }
    }

    /** TEMPORARY: loads the exercises found in the Hevy export. Safe to tap more than once. */
    fun loadHevyExercises() {
        viewModelScope.launch { seedHevyExercises(dao) }
    }
}