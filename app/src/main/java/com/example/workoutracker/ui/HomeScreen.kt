package com.example.workoutracker.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutracker.data.WorkoutEntity
import com.example.workoutracker.data.WorkoutSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenWorkout: (Long) -> Unit,
    onOpenFinishedWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val active by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val lastWorkout by viewModel.lastWorkout.collectAsStateWithLifecycle()
    val weeks by viewModel.weeks.collectAsStateWithLifecycle()
    val current = active

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Workout Tracker") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            if (weeks.isNotEmpty()) ThisWeekCard(weeks)
            lastWorkout?.let { LastWorkoutCard(it, onClick = { onOpenFinishedWorkout(it.id) }) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThisWeekCard(weeks: List<WeekTotals>) {
    val thisWeek = weeks.last()
    val lastWeek = weeks.getOrNull(weeks.lastIndex - 1)
    var metric by rememberSaveable { mutableStateOf(WeekMetric.Time) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("This week", style = MaterialTheme.typography.labelLarge)
            // The headline from your sketch: "6 hours trained this week".
            Text(
                "${formatDuration(thisWeek.durationMillis)} trained",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                listOf(
                    countLabel(thisWeek.workouts, "workout"),
                    countLabel(thisWeek.sets, "set"),
                    "${formatVolume(thisWeek.volumeKg)} kg"
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium
            )
            lastWeek?.let {
                Text(
                    "Last week: ${formatDuration(it.durationMillis)} · ${countLabel(it.workouts, "workout")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekMetric.entries.forEach { m ->
                    FilterChip(selected = metric == m, onClick = { metric = m }, label = { Text(m.label) })
                }
            }
            WeeklyBarChart(weeks = weeks, metric = metric, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastWorkoutCard(summary: WorkoutSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Last workout", style = MaterialTheme.typography.labelLarge)
            Text(summary.title, style = MaterialTheme.typography.titleLarge)
            Text(formatWorkoutDate(summary.startedAt), style = MaterialTheme.typography.bodyMedium)
            Text(
                listOfNotNull(
                    summary.endedAt?.let { formatDuration(it - summary.startedAt) },
                    countLabel(summary.exerciseCount, "exercise"),
                    countLabel(summary.doneSets, "set"),
                    if (summary.volumeKg > 0) "${formatVolume(summary.volumeKg)} kg" else null
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall
            )
            Text("Tap to see more", style = MaterialTheme.typography.labelSmall)
        }
    }
}