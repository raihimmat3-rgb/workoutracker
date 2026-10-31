package com.example.workoutracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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

@Composable
fun ActiveWorkoutScreen(
    onFinished: () -> Unit,
    onDiscarded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = viewModel()
) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    val current = workout

    if (current == null) {
        // Still loading, or just deleted while we navigate away.
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ActiveWorkoutContent(
            workout = current,
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Exercises and sets will appear here (Days 10 and 11).",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
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