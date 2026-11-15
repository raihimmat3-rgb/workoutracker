package com.example.workoutracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutracker.hevy.ConflictKind
import com.example.workoutracker.hevy.Resolution
import com.example.workoutracker.hevy.SyncConflict
import com.example.workoutracker.hevy.SyncResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The system file picker. Hevy's file may be labelled several ways, so accept them all
    // (and anything, as a fallback): the parser rejects files that aren't Hevy exports.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::load)
    }
    val pickFile = {
        picker.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain", "*/*"))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Sync from Hevy") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val s = state) {
                SyncUiState.ChooseFile -> ChooseFileContent(onPick = pickFile)
                is SyncUiState.Working -> WorkingContent(s.message)
                is SyncUiState.Preview -> PreviewContent(
                    preview = s,
                    onChoose = viewModel::choose,
                    onChooseAll = viewModel::chooseForAll,
                    onConfirm = viewModel::confirm,
                    onCancel = onClose
                )
                is SyncUiState.Done -> DoneContent(s.result, onClose)
                is SyncUiState.Failed -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = pickFile) { Text("Choose another file") }
                }
            }
        }
    }
}

@Composable
private fun ChooseFileContent(onPick: () -> Unit) {
    Text(
        "Hevy only lets free accounts export a file, so syncing has two steps:",
        style = MaterialTheme.typography.bodyLarge
    )
    Text("1. In Hevy's settings, export your workouts as a CSV file.", style = MaterialTheme.typography.bodyMedium)
    Text(
        "2. Choose that file here, or share it to Workout Tracker from Hevy or your Files app.",
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        "You'll see what will change before anything is saved. Workouts you logged in this app are never changed.",
        style = MaterialTheme.typography.bodySmall
    )
    Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) { Text("Choose exported file") }
}

@Composable
private fun WorkingContent(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(message)
        }
    }
}

@Composable
private fun PreviewContent(
    preview: SyncUiState.Preview,
    onChoose: (Long, Resolution) -> Unit,
    onChooseAll: (Resolution) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val plan = preview.prepared.plan
    val undecided = plan.conflicts.count { it.localId !in preview.resolutions }

    // ---- What will happen ----
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("What will change", style = MaterialTheme.typography.titleMedium)
            CountRow("New workouts", plan.toAdd.size)
            CountRow("Updated from Hevy", plan.toUpdate.size)
            CountRow("Deleted (removed in Hevy)", plan.toDelete.size)
            CountRow("Need your choice", plan.conflicts.size)
            CountRow("Already up to date", plan.unchanged)
            if (plan.clashes.isNotEmpty()) CountRow("Skipped (same minute as a workout here)", plan.clashes.size)
        }
    }

    // Deleting is the one change worth naming workout by workout before it happens.
    if (plan.toDelete.isNotEmpty()) {
        ExpandableList(
            title = "Will be deleted (${plan.toDelete.size})",
            lines = plan.toDelete.map { id ->
                preview.prepared.imported[id]?.let { "${it.title} · ${formatWorkoutDate(it.startedAt)}" } ?: "Workout $id"
            }
        )
    }

    if (preview.problems.isNotEmpty()) {
        ExpandableList(
            title = "Rows that couldn't be read (${preview.problems.size})",
            lines = preview.problems.map { "Line ${it.line}: ${it.message}" }
        )
    }

    // ---- Your choices ----
    if (plan.conflicts.isNotEmpty()) {
        Text("Choose what to keep", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onChooseAll(Resolution.KeepMine) }) { Text("Keep all mine") }
            OutlinedButton(onClick = { onChooseAll(Resolution.UseHevy) }) { Text("All Hevy's") }
        }
        plan.conflicts.forEach { conflict ->
            ConflictCard(conflict, preview.resolutions[conflict.localId]) { onChoose(conflict.localId, it) }
        }
    }

    // ---- Confirm ----
    if (!plan.hasChanges) {
        Text("Everything is already up to date.", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    } else {
        if (undecided > 0) {
            Text(
                "${countLabel(undecided, "workout")} not decided yet: left as they are, and asked about next sync.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Sync") }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel (nothing changes)") }
    }
}

@Composable
private fun CountRow(label: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("$count", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ExpandableList(title: String, lines: List<String>) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            TextButton(onClick = { open = !open }) { Text(if (open) "Hide: $title" else "Show: $title") }
            if (open) {
                lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
            }
        }
    }
}

@Composable
private fun ConflictCard(conflict: SyncConflict, chosen: Resolution?, onChoose: (Resolution) -> Unit) {
    val (explanation, hevyLabel) = when (conflict.kind) {
        ConflictKind.EditedHere -> "You edited this here. Hevy's copy hasn't changed." to "Use Hevy's"
        ConflictKind.EditedInBoth -> "You edited this here, and it also changed in Hevy." to "Use Hevy's"
        ConflictKind.DeletedInHevy -> "Deleted in Hevy, but you edited it here." to "Delete it"
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(conflict.title, style = MaterialTheme.typography.titleMedium)
            Text(formatWorkoutDate(conflict.startedAt), style = MaterialTheme.typography.bodySmall)
            Text(explanation, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = chosen == Resolution.KeepMine,
                    onClick = { onChoose(Resolution.KeepMine) },
                    label = { Text("Keep mine") }
                )
                FilterChip(
                    selected = chosen == Resolution.UseHevy,
                    onClick = { onChoose(Resolution.UseHevy) },
                    label = { Text(hevyLabel) }
                )
            }
        }
    }
}

@Composable
private fun DoneContent(result: SyncResult, onClose: () -> Unit) {
    Text("Sync complete", style = MaterialTheme.typography.headlineSmall)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CountRow("Added", result.added)
            CountRow("Updated", result.updated)
            CountRow("Deleted", result.deleted)
            CountRow("Kept your version", result.keptYours)
            if (result.undecided > 0) CountRow("Left for next time", result.undecided)
            if (result.skipped > 0) CountRow("Skipped", result.skipped)
        }
    }
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}