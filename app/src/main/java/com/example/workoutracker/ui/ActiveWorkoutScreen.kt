package com.example.workoutracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import com.example.workoutracker.data.WorkoutEntity
import com.example.workoutracker.data.WorkoutExerciseWithSets

@Composable
fun ActiveWorkoutScreen(
    onFinished: () -> Unit,
    onDiscarded: () -> Unit,
    onAddExercise: (workoutId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = viewModel()
) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val current = workout

    if (current == null) {
        // Still loading, or just deleted while we navigate away.
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ActiveWorkoutContent(
            workout = current,
            exercises = exercises,
            onAddExercise = { onAddExercise(current.id) },
            onRemoveExercise = viewModel::removeExercise,
            onFinish = { title, notes -> viewModel.finish(title, notes, onFinished) },
            onDiscard = { viewModel.discard(onDiscarded) },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveWorkoutContent(
    workout: WorkoutEntity,
    exercises: List<WorkoutExerciseWithSets>,
    onAddExercise: () -> Unit,
    onRemoveExercise: (workoutExerciseId: Long) -> Unit,
    onFinish: (title: String, notes: String) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state, seeded once from the database. Writing every keystroke to the database and
    // reading it straight back makes the cursor jump, so we save when the user taps Finish.
    // rememberSaveable keeps the text across rotation and process death.
    var title by rememberSaveable { mutableStateOf(workout.title) }
    var notes by rememberSaveable { mutableStateOf(workout.notes ?: "") }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    // The id of an exercise waiting for "remove?" confirmation, or null.
    var pendingRemoval by rememberSaveable { mutableStateOf<Long?>(null) }
    val elapsed = rememberElapsedMillis(workout.startedAt)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Active workout") },
                actions = {
                    Button(
                        onClick = { confirmFinish = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Finish") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(formatElapsed(elapsed), style = MaterialTheme.typography.displayMedium)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Workout title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (exercises.isEmpty()) {
                Text(
                    "No exercises yet. Tap Add exercise to start.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            exercises.forEach { item ->
                ExerciseBlock(
                    item = item,
                    onRemove = {
                        // Nothing to lose if it has no sets, so skip the confirmation.
                        if (item.sets.isEmpty()) onRemoveExercise(item.workoutExercise.id)
                        else pendingRemoval = item.workoutExercise.id
                    }
                )
            }

            FilledTonalButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add exercise")
            }

            OutlinedButton(
                onClick = { confirmDiscard = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Discard workout") }
        }
    }

    if (confirmFinish) {
        val shownTitle = title.trim().ifEmpty { workout.title }
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish workout?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("\"$shownTitle\" will be saved to your history.")
                    Text("Duration: ${formatElapsed(elapsed)}")
                    if (isSuspiciouslyShort(elapsed)) {
                        Text(
                            "This workout is under a minute long. Did you mean to finish it?",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmFinish = false; onFinish(title, notes) }) { Text("Finish") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFinish = false }) { Text("Keep going") }
            }
        )
    }

    pendingRemoval?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove exercise?") },
            text = { Text("Its logged sets will be deleted too.") },
            confirmButton = {
                TextButton(onClick = { pendingRemoval = null; onRemoveExercise(id) }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Keep") }
            }
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this workout?") },
            text = { Text("Everything logged in it will be deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDiscard() }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep") }
            }
        )
    }
}

/** One exercise in the active workout. Set logging is added on Day 11. */
@Composable
private fun ExerciseBlock(item: WorkoutExerciseWithSets, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
                item.exercise.muscleGroup?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = when (item.sets.size) {
                        0 -> "No sets yet"
                        1 -> "1 set"
                        else -> "${item.sets.size} sets"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove ${item.exercise.name}")
            }
        }
    }
}