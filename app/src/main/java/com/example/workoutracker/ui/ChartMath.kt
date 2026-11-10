package com.example.workoutracker.ui

import com.example.workoutracker.data.ExerciseSession
import kotlin.math.abs
import kotlin.math.max

/** One dot on the chart: x is a time (epoch millis), y is the value. */
data class ChartPoint(val x: Long, val y: Double)

/** What the chart can plot. Each knows how to read its number from a session and how to show it. */
enum class ChartMetric(val label: String) {
    OneRepMax("Est. 1RM"),
    Heaviest("Heaviest"),
    Volume("Volume"),
    Reps("Most reps");

    fun read(session: ExerciseSession): Double? = when (this) {
        OneRepMax -> session.bestOneRepMaxKg
        Heaviest -> session.heaviestKg
        Volume -> session.volumeKg.takeIf { it > 0 } // 0 means bodyweight-only: nothing to plot
        Reps -> session.mostReps?.toDouble()
    }

    fun format(value: Double): String = when (this) {
        Reps -> "${value.toInt()} reps"
        else -> "${formatVolume(value)} kg"
    }
}

/** How far back the chart looks. null days = everything. */
enum class ChartRange(val label: String, val days: Long?) {
    ThreeMonths("3M", 91),
    Year("1Y", 365),
    All("All", null)
}

/** Turns sessions into points for [metric], skipping sessions that have no value for it. */
fun chartPoints(sessions: List<ExerciseSession>, metric: ChartMetric): List<ChartPoint> =
    sessions.mapNotNull { s -> metric.read(s)?.let { ChartPoint(s.startedAt, it) } }

/** Keeps only points inside [range], counting back from [now]. */
fun filterByRange(points: List<ChartPoint>, range: ChartRange, now: Long): List<ChartPoint> {
    val days = range.days ?: return points
    val cutoff = now - days * 24 * 60 * 60 * 1000
    return points.filter { it.x >= cutoff }
}

/**
 * The y-axis bounds: the data's range plus 10% padding so the line doesn't touch the edges.
 * Never goes below 0, and a flat line (all values equal) gets some room either side.
 */
fun axisRange(minY: Double, maxY: Double): Pair<Double, Double> {
    val span = maxY - minY
    val pad = if (span < 1e-9) max(abs(maxY) * 0.1, 5.0) else span * 0.1
    return max(0.0, minY - pad) to (maxY + pad)
}

/** Where [value] sits between [min] and [max], from 0 to 1. Middle (0.5) if there's no range. */
fun fraction(value: Double, min: Double, max: Double): Float =
    if (max <= min) 0.5f else ((value - min) / (max - min)).toFloat()

/** Index of the position in [positions] closest to [target] (used to pick the tapped dot). */
fun nearestIndex(positions: List<Float>, target: Float): Int? =
    positions.indices.minByOrNull { abs(positions[it] - target) }