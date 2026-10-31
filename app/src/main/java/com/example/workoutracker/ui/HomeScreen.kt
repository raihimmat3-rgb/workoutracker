package com.example.workoutracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutracker.data.WorkoutEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val active by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val current = active

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Workout Tracker") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (current != null) {
                ResumeCard(workout = current, onResume = { onOpenWorkout(current.id) })
            } else {
                Button(
                    onClick = { viewModel.startWorkout(onOpenWorkout) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Text("Start a workout", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ResumeCard(workout: WorkoutEntity, onResume: () -> Unit) {
    val elapsed = rememberElapsedMillis(workout.startedAt)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Workout in progress", style = MaterialTheme.typography.labelLarge)
            Text(workout.title, style = MaterialTheme.typography.titleLarge)
            Text(formatElapsed(elapsed), style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                Text("Resume workout")
            }
        }
    }
}