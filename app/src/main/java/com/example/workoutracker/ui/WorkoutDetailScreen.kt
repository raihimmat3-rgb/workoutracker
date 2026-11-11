package com.example.workoutracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.workoutracker.data.WorkoutExerciseWithSets
import com.example.workoutracker.data.WorkoutSetEntity
import com.example.workoutracker.data.WorkoutWithDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onEdit: (workoutId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutDetailViewModel = viewModel()
) {
    val details by viewModel.details.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showSaveTemplate by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    details?.let { d ->
                        IconButton(onClick = { onEdit(d.workout.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit workout")
                        }
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete workout")
                    }
                }
            )
        }
    ) { padding ->
        val current = details
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { WorkoutHeader(current) }
                item {
                    OutlinedButton(onClick = { showSaveTemplate = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Save as routine")
                    }
                }
                items(current.exercises, key = { it.workoutExercise.id }) { item ->
                    ExerciseSummaryCard(item)
                }
            }
        }
    }

    if (showSaveTemplate) {
        details?.let { d ->
            SaveTemplateDialog(
                defaultName = d.workout.title,
                onSave = { name -> showSaveTemplate = false; viewModel.saveAsTemplate(name) },
                onDismiss = { showSaveTemplate = false }
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this workout?") },
            text = { Text("It will be removed from your history, with all its sets. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onDeleted) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun WorkoutHeader(details: WorkoutWithDetails) {
    val workout = details.workout
    val totalVolume = details.exercises.sumOf { exerciseVolume(it.sets) }
    val doneSets = details.exercises.sumOf { item -> item.sets.count { it.isDone } }
    val stats = listOfNotNull(
        workout.endedAt?.let { formatDuration(it - workout.startedAt) },
        countLabel(doneSets, "set"),
        if (totalVolume > 0) "${formatVolume(totalVolume)} kg" else null
    ).joinToString(" · ")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(workout.title, style = MaterialTheme.typography.headlineSmall)
        Text(formatWorkoutDate(workout.startedAt), style = MaterialTheme.typography.bodyMedium)
        Text(stats, style = MaterialTheme.typography.bodyMedium)
        workout.notes?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ExerciseSummaryCard(item: WorkoutExerciseWithSets) {
    val volume = exerciseVolume(item.sets)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
            if (volume > 0) {
                Text("Volume: ${formatVolume(volume)} kg", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            item.sets.forEachIndexed { index, set -> SetSummaryRow(number = index + 1, set = set) }
        }
    }
}

@Composable
private fun SetSummaryRow(number: Int, set: WorkoutSetEntity) {
    // Unticked sets were kept on finish but are shown faded and labelled, so it's clear
    // they don't count towards volume or the Previous column.
    val colour = if (set.isDone) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$number", color = colour, modifier = Modifier.width(32.dp))
        // formatPrevious gives the same "60kg × 8" wording the active workout uses.
        Text(formatPrevious(set.weightKg, set.reps), color = colour, modifier = Modifier.weight(1f))
        if (set.isDone) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Done",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text("not done", color = colour, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SaveTemplateDialog(defaultName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(defaultName) }
    // Same rules as an exercise name: not blank, at most 60 characters.
    val error = validateExerciseName(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Saves these exercises, in this order, with the same number of sets. " +
                            "Weights and reps aren't saved: starting the routine fills them in from last time.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Routine name") },
                    singleLine = true,
                    isError = error != null && name.isNotEmpty(),
                    supportingText = error?.takeIf { name.isNotEmpty() }?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = error == null) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}