package com.example.workoutracker.ui

import com.example.workoutracker.data.WorkoutSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WeeklyStatsTest {

    private val london = ZoneId.of("Europe/London")
    private fun at(y: Int, mo: Int, d: Int, h: Int = 18, mi: Int = 0) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(london).toInstant().toEpochMilli()

    private fun workout(start: Long, minutes: Long, sets: Int, volume: Double) = WorkoutSummary(
        id = start, title = "w", startedAt = start, endedAt = start + minutes * 60_000,
        source = "manual", exerciseCount = 1, doneSets = sets, volumeKg = volume
    )

    @Test
    fun weekStart_isTheMonday() {
        // Tue 10 Nov 2026 -> Mon 9 Nov; a Monday stays itself; Sunday goes back 6 days.
        assertEquals(LocalDate.of(2026, 11, 9), weekStartOf(at(2026, 11, 10), london))
        assertEquals(LocalDate.of(2026, 11, 9), weekStartOf(at(2026, 11, 9, 0, 5), london))
        assertEquals(LocalDate.of(2026, 11, 9), weekStartOf(at(2026, 11, 15, 23, 30), london))
    }

    @Test
    fun weeklyTotals_addUpEachWeekAndKeepEmptyWeeks() {
        val now = at(2026, 11, 10)
        val summaries = listOf(
            workout(at(2026, 11, 9), minutes = 60, sets = 15, volume = 5000.0),  // this week
            workout(at(2026, 11, 10), minutes = 45, sets = 12, volume = 4000.0), // this week
            workout(at(2026, 10, 27), minutes = 70, sets = 20, volume = 6000.0)  // 2 weeks ago
        )

        val weeks = weeklyTotals(summaries, london, now, weeks = 3)

        assertEquals(listOf(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9)),
            weeks.map { it.weekStart })
        assertEquals(listOf(1, 0, 2), weeks.map { it.workouts })
        assertEquals(105 * 60_000L, weeks[2].durationMillis)
        assertEquals(27, weeks[2].sets)
        assertEquals(9000.0, weeks[2].volumeKg, 0.0001)
    }

    @Test
    fun weeklyTotals_ignoreWorkoutsOlderThanTheWindow() {
        val now = at(2026, 11, 10)
        val old = listOf(workout(at(2026, 1, 5), 60, 10, 1000.0))
        assertEquals(0, weeklyTotals(old, london, now, weeks = 8).sumOf { it.workouts })
    }

    @Test
    fun weekMetric_formats() {
        assertEquals("1h 5m", WeekMetric.Time.format(65 * 60_000.0))
        assertEquals("3 workouts", WeekMetric.Workouts.format(3.0))
        assertEquals("12400 kg", WeekMetric.Volume.format(12400.0))
    }
}