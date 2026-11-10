package com.example.workoutracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.workoutracker.data.SetRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state?.exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val current = state
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExerciseInfo(current)
                ProgressCard(current)
                PersonalBestsCard(current.bests)
                current.exercise.instructions?.let { HowToCard(it) }
            }
        }
    }
}

@Composable
private fun ExerciseInfo(state: ExerciseDetailState) {
    val exercise = state.exercise
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOfNotNull(exercise.muscleGroup, exercise.equipment)
            .takeIf { it.isNotEmpty() }
            ?.let { Text(it.joinToString(" · "), style = MaterialTheme.typography.bodyMedium) }

        val stats = state.stats
        val last = stats.lastPerformedAt
        Text(
            if (last == null) "Not done yet"
            else "Done in ${countLabel(stats.workoutCount, "workout")} · last on ${formatWorkoutDate(last)}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressCard(state: ExerciseDetailState) {
    // Weighted exercises open on estimated 1RM; bodyweight ones (no weights at all) on reps.
    val defaultMetric =
        if (state.sessions.any { it.heaviestKg != null }) ChartMetric.OneRepMax else ChartMetric.Reps
    var metric by rememberSaveable { mutableStateOf(defaultMetric) }
    var range by rememberSaveable { mutableStateOf(ChartRange.All) }

    val points = filterByRange(chartPoints(state.sessions, metric), range, System.currentTimeMillis())

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Progress", style = MaterialTheme.typography.titleMedium)

            // Scrolls sideways if the chips don't fit on a narrow phone.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartMetric.entries.forEach { m ->
                    FilterChip(selected = metric == m, onClick = { metric = m }, label = { Text(m.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChartRange.entries.forEach { r ->
                    FilterChip(selected = range == r, onClick = { range = r }, label = { Text(r.label) })
                }
            }

            if (points.isEmpty()) {
                Text(
                    if (state.sessions.isEmpty()) "Finish a workout with a ticked set of this exercise to start your chart."
                    else "Nothing to show for ${metric.label.lowercase()} in this time range.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LineChart(
                    points = points,
                    formatValue = { metric.format(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (points.size == 1) {
                    Text(
                        "One workout so far. The line appears from your second.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalBestsCard(bests: PersonalBests) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Personal bests", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (bests.isEmpty) {
                Text(
                    "Finish a workout with a ticked set of this exercise to see your records here.",
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            bests.heaviest?.let { r ->
                RecordRow("Heaviest set", formatPrevious(r.weightKg, r.reps), r)
            }
            bests.bestOneRepMax?.let { r ->
                val estimate = estimateOneRepMax(r.weightKg ?: 0.0, r.reps ?: 0)
                RecordRow(
                    "Est. one-rep max",
                    "${formatVolume(estimate)} kg (from ${formatPrevious(r.weightKg, r.reps)})",
                    r
                )
            }
            bests.mostReps?.let { r ->
                val at = r.weightKg?.let { " at ${formatWeight(it)}kg" } ?: ""
                RecordRow("Most reps", "${r.reps} reps$at", r)
            }
            bests.bestVolume?.let { r ->
                RecordRow(
                    "Best set volume",
                    "${formatVolume(setVolume(r.weightKg, r.reps))} kg (${formatPrevious(r.weightKg, r.reps)})",
                    r
                )
            }
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String, record: SetRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1.4f)) {
            Text(value, style = MaterialTheme.typography.bodyLarge)
            Text(formatWorkoutDate(record.startedAt), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HowToCard(instructions: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("How to", style = MaterialTheme.typography.titleMedium)
            Text(
                instructions,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}