package com.example.workoutracker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutOrderTest {

    @Test
    fun middleItem_movesEitherWay() {
        assertEquals(0, neighbourIndex(index = 1, size = 3, up = true))
        assertEquals(2, neighbourIndex(index = 1, size = 3, up = false))
    }

    @Test
    fun firstItem_cantMoveUp() {
        assertNull(neighbourIndex(index = 0, size = 3, up = true))
    }

    @Test
    fun lastItem_cantMoveDown() {
        assertNull(neighbourIndex(index = 2, size = 3, up = false))
    }

    @Test
    fun missingItem_cantMove() {
        // indexOfFirst returns -1 when the item isn't found.
        assertNull(neighbourIndex(index = -1, size = 3, up = false))
        assertNull(neighbourIndex(index = 0, size = 0, up = false))
    }

    @Test
    fun onlyItem_cantMoveAtAll() {
        assertNull(neighbourIndex(index = 0, size = 1, up = true))
        assertNull(neighbourIndex(index = 0, size = 1, up = false))
    }
}