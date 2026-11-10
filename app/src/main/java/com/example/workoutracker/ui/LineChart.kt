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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Space kept free for the axis labels, around the plotting area.
private val LEFT_GUTTER = 48.dp
private val BOTTOM_GUTTER = 20.dp
private val TOP_PAD = 8.dp

/**
 * A simple line chart drawn by hand on a Canvas, so there's no charting library to learn
 * or keep up to date. X is time-proportional (a long gap between workouts looks long).
 * Tap near a dot to see its exact value and date above the chart.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val textMeasurer = rememberTextMeasurer()

    // Which dot is selected. Starts on the latest; resets whenever the data changes.
    var selected by remember(points) { mutableStateOf(points.lastIndex) }
    val (yMin, yMax) = remember(points) { axisRange(points.minOf { it.y }, points.maxOf { it.y }) }
    val xMin = points.first().x.toDouble()
    val xMax = points.last().x.toDouble()

    Column(modifier = modifier) {
        val shown = points[selected.coerceIn(points.indices)]
        Text(
            "${formatValue(shown.y)} · ${formatChartDate(shown.x)}",
            style = MaterialTheme.typography.titleSmall
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(points) {
                    detectTapGestures { tap ->
                        val left = LEFT_GUTTER.toPx()
                        val width = size.width - left
                        val xs = points.map { left + fraction(it.x.toDouble(), xMin, xMax) * width }
                        nearestIndex(xs, tap.x)?.let { selected = it }
                    }
                }
        ) {
            val left = LEFT_GUTTER.toPx()
            val top = TOP_PAD.toPx()
            val bottom = size.height - BOTTOM_GUTTER.toPx()
            val width = size.width - left
            val height = bottom - top

            fun toPx(p: ChartPoint) = Offset(
                x = left + fraction(p.x.toDouble(), xMin, xMax) * width,
                y = bottom - fraction(p.y, yMin, yMax) * height
            )

            // Three horizontal grid lines (bottom, middle, top), each labelled with its value.
            for (i in 0..2) {
                val value = yMin + (yMax - yMin) * i / 2
                val y = bottom - height * i / 2f
                drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val label = textMeasurer.measure(formatValue(value), labelStyle)
                drawText(label, topLeft = Offset(0f, y - label.size.height / 2f))
            }

            // The line through every point, then a dot on each.
            if (points.size > 1) {
                val path = Path().apply {
                    points.forEachIndexed { i, p ->
                        val o = toPx(p)
                        if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                    }
                }
                drawPath(
                    path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            points.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = toPx(it)) }
            drawCircle(
                lineColor,
                radius = 8.dp.toPx(),
                center = toPx(points[selected.coerceIn(points.indices)]),
                style = Stroke(width = 2.dp.toPx())
            )

            // Dates of the first and last points along the bottom.
            val first = textMeasurer.measure(formatChartDate(points.first().x), labelStyle)
            drawText(first, topLeft = Offset(left, bottom + 4.dp.toPx()))
            if (points.size > 1) {
                val last = textMeasurer.measure(formatChartDate(points.last().x), labelStyle)
                drawText(last, topLeft = Offset(size.width - last.size.width, bottom + 4.dp.toPx()))
            }
        }
    }
}

/** e.g. "3 Nov 26". Short, because it sits under the chart. */
private fun formatChartDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yy", Locale.getDefault()))