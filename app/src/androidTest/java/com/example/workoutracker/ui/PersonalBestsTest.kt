package com.example.workoutracker.ui

import com.example.workoutracker.data.SetRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalBestsTest {

    @Test
    fun oneRep_isExactlyTheWeight() {
        assertEquals(140.0, estimateOneRepMax(140.0, 1), 0.0001)
    }

    @Test
    fun epley_forSeveralReps() {
        // 100 x (1 + 5/30) = 116.67
        assertEquals(116.6667, estimateOneRepMax(100.0, 5), 0.001)
        // 60 x (1 + 10/30) = 80
        assertEquals(80.0, estimateOneRepMax(60.0, 10), 0.0001)
    }

    @Test
    fun zeroReps_isZero() {
        assertEquals(0.0, estimateOneRepMax(100.0, 0), 0.0001)
    }

    @Test
    fun personalBests_isEmptyOnlyWhenNothingIsSet() {
        assertEquals(true, PersonalBests(null, null, null, null).isEmpty)
        val record = SetRecord(weightKg = 60.0, reps = 8, startedAt = 0, workoutId = 1)
        assertEquals(false, PersonalBests(record, null, null, null).isEmpty)
    }
}