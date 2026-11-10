package com.example.workoutracker.ui

import com.example.workoutracker.data.ExerciseSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMathTest {

    private val day = 24L * 60 * 60 * 1000

    private fun session(at: Long, heaviest: Double?, oneRm: Double?, volume: Double, reps: Int?) =
        ExerciseSession(workoutId = at, startedAt = at, heaviestKg = heaviest,
            bestOneRepMaxKg = oneRm, volumeKg = volume, mostReps = reps)

    @Test
    fun chartPoints_readTheChosenMetric() {
        val sessions = listOf(session(1, 100.0, 116.7, 900.0, 8), session(2, 105.0, 120.0, 950.0, 6))
        assertEquals(listOf(ChartPoint(1, 100.0), ChartPoint(2, 105.0)), chartPoints(sessions, ChartMetric.Heaviest))
        assertEquals(listOf(ChartPoint(1, 8.0), ChartPoint(2, 6.0)), chartPoints(sessions, ChartMetric.Reps))
    }

    @Test
    fun chartPoints_skipSessionsWithNoValue() {
        // Bodyweight-only session: no weight, zero volume, but it has reps.
        val sessions = listOf(session(1, null, null, 0.0, 12), session(2, 10.0, 14.0, 120.0, 12))
        assertEquals(1, chartPoints(sessions, ChartMetric.OneRepMax).size)
        assertEquals(1, chartPoints(sessions, ChartMetric.Volume).size)
        assertEquals(2, chartPoints(sessions, ChartMetric.Reps).size)
    }

    @Test
    fun filterByRange_keepsRecentPoints() {
        val now = 1_000 * day
        val points = listOf(ChartPoint(now - 400 * day, 1.0), ChartPoint(now - 100 * day, 2.0), ChartPoint(now - 10 * day, 3.0))
        assertEquals(1, filterByRange(points, ChartRange.ThreeMonths, now).size)
        assertEquals(2, filterByRange(points, ChartRange.Year, now).size)
        assertEquals(3, filterByRange(points, ChartRange.All, now).size)
    }

    @Test
    fun axisRange_padsAndNeverGoesNegative() {
        val (low, high) = axisRange(100.0, 200.0)
        assertEquals(90.0, low, 0.0001)
        assertEquals(210.0, high, 0.0001)
        assertTrue(axisRange(1.0, 50.0).first >= 0.0)
    }

    @Test
    fun axisRange_givesAFlatLineSomeRoom() {
        val (low, high) = axisRange(60.0, 60.0)
        assertTrue(low < 60.0 && high > 60.0)
    }

    @Test
    fun fraction_isMiddleWhenThereIsNoRange() {
        assertEquals(0.5f, fraction(5.0, 5.0, 5.0), 0.0001f)
        assertEquals(0.25f, fraction(25.0, 0.0, 100.0), 0.0001f)
    }

    @Test
    fun nearestIndex_findsClosest() {
        assertEquals(1, nearestIndex(listOf(0f, 50f, 100f), 60f))
        assertNull(nearestIndex(emptyList(), 10f))
    }
}