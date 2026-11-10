package com.example.workoutracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

private val LABEL_SPACE = 18.dp
private val dayMonth = DateTimeFormatter.ofPattern("d/M")

/**
 * One bar per week, oldest on the left, this week highlighted. Tap a bar to see its value.
 * Drawn by hand on a Canvas, like the progress chart.
 */
@Composable
fun WeeklyBarChart(
    weeks: List<WeekTotals>,
    metric: WeekMetric,
    modifier: Modifier = Modifier
) {
    val currentColor = MaterialTheme.colorScheme.primary
    val pastColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val textMeasurer = rememberTextMeasurer()
    var selected by remember(weeks, metric) { mutableStateOf(weeks.lastIndex) }

    Column(modifier = modifier) {
        val shown = weeks[selected.coerceIn(weeks.indices)]
        val which = if (selected == weeks.lastIndex) "This week" else "Week of ${shown.weekStart.format(dayMonth)}"
        Text("$which: ${metric.format(metric.read(shown))}", style = MaterialTheme.typography.titleSmall)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(weeks, metric) {
                    detectTapGestures { tap ->
                        val slot = size.width / weeks.size.toFloat()
                        selected = (tap.x / slot).toInt().coerceIn(weeks.indices)
                    }
                }
        ) {
            val maxValue = weeks.maxOf { metric.read(it) }
            val chartBottom = size.height - LABEL_SPACE.toPx()
            val slot = size.width / weeks.size
            val barWidth = slot * 0.6f

            weeks.forEachIndexed { i, week ->
                val left = i * slot + (slot - barWidth) / 2
                // An empty week still gets a thin stub, so it's clear the week exists.
                val height = if (maxValue <= 0.0) 0f
                else (metric.read(week) / maxValue).toFloat() * chartBottom
                val drawn = maxOf(height, 2.dp.toPx())
                drawRoundRect(
                    color = if (i == weeks.lastIndex) currentColor else pastColor,
                    topLeft = Offset(left, chartBottom - drawn),
                    size = Size(barWidth, drawn),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                if (i == selected) {
                    drawRoundRect(
                        color = currentColor,
                        topLeft = Offset(left - 2.dp.toPx(), chartBottom - drawn - 2.dp.toPx()),
                        size = Size(barWidth + 4.dp.toPx(), drawn + 4.dp.toPx()),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                val label = textMeasurer.measure(week.weekStart.format(dayMonth), labelStyle)
                drawText(
                    label,
                    topLeft = Offset(i * slot + (slot - label.size.width) / 2, chartBottom + 4.dp.toPx())
                )
            }
        }
    }
}