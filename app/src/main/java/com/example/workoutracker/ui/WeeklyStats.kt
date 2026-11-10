package com.example.workoutracker.ui

import com.example.workoutracker.data.WorkoutSummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Totals for one Monday-to-Sunday week. */
data class WeekTotals(
    val weekStart: LocalDate,
    val workouts: Int,
    val durationMillis: Long,
    val sets: Int,
    val volumeKg: Double
)

/** The Monday of the week [epochMillis] falls in, in [zone] (so a late Sunday workout counts as Sunday). */
fun weekStartOf(epochMillis: Long, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(epochMillis)
        .atZone(zone)
        .toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Totals for the last [weeks] weeks up to and including this one, oldest first.
 * Weeks with no workouts are included as zeros, so a bar chart shows the gaps.
 * Done in Kotlin rather than SQL because "which week?" depends on your time zone,
 * which SQLite doesn't know about.
 */
fun weeklyTotals(
    summaries: List<WorkoutSummary>,
    zone: ZoneId,
    now: Long,
    weeks: Int
): List<WeekTotals> {
    val thisWeek = weekStartOf(now, zone)
    val byWeek = summaries.groupBy { weekStartOf(it.startedAt, zone) }
    return (weeks - 1 downTo 0).map { back ->
        val start = thisWeek.minusWeeks(back.toLong())
        val inWeek = byWeek[start].orEmpty()
        WeekTotals(
            weekStart = start,
            workouts = inWeek.size,
            durationMillis = inWeek.sumOf { s -> s.endedAt?.let { it - s.startedAt } ?: 0L },
            sets = inWeek.sumOf { it.doneSets },
            volumeKg = inWeek.sumOf { it.volumeKg }
        )
    }
}

/** What the weekly bar chart can show. */
enum class WeekMetric(val label: String) {
    Time("Time"),
    Volume("Volume"),
    Workouts("Workouts");

    fun read(week: WeekTotals): Double = when (this) {
        Time -> week.durationMillis.toDouble()
        Volume -> week.volumeKg
        Workouts -> week.workouts.toDouble()
    }

    fun format(value: Double): String = when (this) {
        Time -> formatDuration(value.toLong())
        Volume -> "${formatVolume(value)} kg"
        Workouts -> countLabel(value.toInt(), "workout")
    }
}