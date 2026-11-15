package com.example.workoutracker.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.hevy.HevyCsvParser
import com.example.workoutracker.hevy.HevyFormatException
import com.example.workoutracker.hevy.HevySync
import com.example.workoutracker.hevy.PreparedSync
import com.example.workoutracker.hevy.Resolution
import com.example.workoutracker.hevy.RowProblem
import com.example.workoutracker.hevy.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "WorkoutTracker"

/** Every screen the sync can be on. Exactly one at a time. */
sealed interface SyncUiState {
    data object ChooseFile : SyncUiState
    data class Working(val message: String) : SyncUiState
    data class Preview(
        val prepared: PreparedSync,
        val problems: List<RowProblem>,
        val resolutions: Map<Long, Resolution>
    ) : SyncUiState
    data class Done(val result: SyncResult) : SyncUiState
    data class Failed(val message: String) : SyncUiState
}

class SyncViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val sync = HevySync(AppDatabase.get(application))

    private val _state = MutableStateFlow<SyncUiState>(SyncUiState.ChooseFile)
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    init {
        // Shared from Hevy or a file manager: start straight away.
        savedStateHandle.get<String>("uri")?.let { load(Uri.parse(it)) }
    }

    /** Reads, parses and compares the file. Nothing in the database changes yet. */
    fun load(uri: Uri) {
        _state.value = SyncUiState.Working("Reading the file…")
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.use { it.bufferedReader().readText() }
                } ?: throw HevyFormatException("Couldn't open that file")

                _state.value = SyncUiState.Working("Comparing with your workouts…")
                // Parsing and fingerprinting 400+ workouts takes a moment: keep it off the main thread
                // so the screen doesn't freeze.
                val (parsed, prepared) = withContext(Dispatchers.Default) {
                    val parsed = HevyCsvParser().parse(text)
                    parsed to sync.prepare(parsed.workouts)
                }
                _state.value = SyncUiState.Preview(prepared, parsed.problems, emptyMap())
            } catch (e: HevyFormatException) {
                _state.value = SyncUiState.Failed(e.message ?: "That doesn't look like a Hevy export")
            } catch (e: Exception) {
                Log.e(TAG, "sync load failed", e)
                _state.value = SyncUiState.Failed("Something went wrong reading the file: ${e.message}")
            }
        }
    }

    fun choose(localId: Long, resolution: Resolution) = updatePreview { preview ->
        preview.copy(resolutions = preview.resolutions + (localId to resolution))
    }

    /** "Keep all mine" / "Use Hevy's for all". */
    fun chooseForAll(resolution: Resolution) = updatePreview { preview ->
        preview.copy(resolutions = preview.prepared.plan.conflicts.associate { it.localId to resolution })
    }

    fun confirm() {
        val preview = _state.value as? SyncUiState.Preview ?: return
        _state.value = SyncUiState.Working("Syncing…")
        viewModelScope.launch {
            try {
                val result = sync.apply(preview.prepared, preview.resolutions)
                Log.d(TAG, "sync applied: $result")
                _state.value = SyncUiState.Done(result)
            } catch (e: Exception) {
                // apply() runs in one transaction, so a failure here has changed nothing.
                Log.e(TAG, "sync apply failed", e)
                _state.value = SyncUiState.Failed("Sync failed, and nothing was changed: ${e.message}")
            }
        }
    }

    fun startOver() {
        _state.value = SyncUiState.ChooseFile
    }

    private fun updatePreview(change: (SyncUiState.Preview) -> SyncUiState.Preview) {
        _state.update { if (it is SyncUiState.Preview) change(it) else it }
    }
}