package com.example.workoutracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutracker.data.WorkoutEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    // Re-reads whenever the database changes, and stops collecting when the app is in the background.
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("History") },
                actions = {
                    TextButton(onClick = viewModel::addSampleData) { Text("Add sample data") }
                }
            )
        }
    ) { padding ->
        if (workouts.isEmpty()) {
            EmptyState(padding)
        } else {
            WorkoutList(workouts = workouts, padding = padding, onDelete = viewModel::delete)
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text("No workouts yet", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WorkoutList(
    workouts: List<WorkoutEntity>,
    padding: PaddingValues,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // key = id lets Compose track each row correctly when the list changes.
        items(workouts, key = { it.id }) { workout ->
            WorkoutCard(workout = workout, onDelete = { onDelete(workout.id) })
        }
    }
}

@Composable
private fun WorkoutCard(workout: WorkoutEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.title, style = MaterialTheme.typography.titleMedium)
                Text(formatStart(workout.startedAt), style = MaterialTheme.typography.bodyMedium)
                workout.endedAt?.let { end ->
                    val minutes = (end - workout.startedAt) / 60_000
                    Text("$minutes min", style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.getDefault())

private fun formatStart(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)