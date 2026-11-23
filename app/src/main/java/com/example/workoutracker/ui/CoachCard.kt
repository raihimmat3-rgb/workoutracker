package com.example.workoutracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.workoutracker.coach.CoachReviewDto

/** The Coach box from your Day 3 sketch, on a finished workout's screen. */
@Composable
fun CoachCard(state: CoachUiState, onAsk: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Coach", style = MaterialTheme.typography.titleMedium)

            when (state) {
                CoachUiState.NotSetUp -> Text(
                    "The coach is off. Add coach.baseUrl and coach.appKey to local.properties to turn it on.",
                    style = MaterialTheme.typography.bodyMedium
                )

                CoachUiState.Idle -> {
                    Text(
                        "Get a review of this workout: what went well, and why any reps dropped.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onAsk) { Text("Ask the coach") }
                }

                CoachUiState.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("The coach is thinking…", style = MaterialTheme.typography.bodyMedium)
                }

                is CoachUiState.Ready -> ReadyContent(state, onAsk)

                is CoachUiState.Failed -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    state.previous?.let { ReviewContent(it.review) }
                    Button(onClick = onAsk) { Text("Try again") }
                }
            }
        }
    }
}

@Composable
private fun ReadyContent(state: CoachUiState.Ready, onAsk: () -> Unit) {
    ReviewContent(state.review)
    if (state.outdated) {
        Text(
            "You've edited this workout since this review.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
    TextButton(onClick = onAsk) {
        Text(if (state.outdated) "Update the review" else "Ask again (uses 1 of today's requests)")
    }
    Text(
        "AI-generated on ${formatWorkoutDate(state.createdAt)}. General training information, not medical advice.",
        style = MaterialTheme.typography.labelSmall
    )
}

@Composable
private fun ReviewContent(review: CoachReviewDto) {
    Text(review.headline, style = MaterialTheme.typography.titleLarge)
    if (review.summary.isNotBlank()) Text(review.summary, style = MaterialTheme.typography.bodyMedium)

    review.exercises.forEach { note ->
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
            Text(
                trendSymbol(note.trend),
                color = trendColour(note.trend),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(24.dp)
            )
            Column {
                Text(note.name, style = MaterialTheme.typography.titleSmall)
                Text(note.note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (review.tips.isNotEmpty()) {
        Text("Next time", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
        review.tips.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    }
}

private fun trendSymbol(trend: String) = when (trend) {
    "up" -> "▲"    // ▲
    "down" -> "▼"  // ▼
    "new" -> "★"   // ★
    else -> "="
}

@Composable
private fun trendColour(trend: String) = when (trend) {
    "up" -> MaterialTheme.colorScheme.primary
    "down" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}