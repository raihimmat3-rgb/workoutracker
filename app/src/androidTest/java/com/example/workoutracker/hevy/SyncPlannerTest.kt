package com.example.workoutracker.hevy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPlannerTest {

    private fun hevy(start: Long, reps: Int = 5, title: String = "W$start") = HevyWorkout(
        title = title, startedAt = start, endedAt = start + 3_600_000, description = null,
        exercises = listOf(HevyExercise("Squat", null, listOf(HevySet(0, "normal", 100.0, reps, null, null, null))))
    )

    private fun fp(w: HevyWorkout) = w.toContent().fingerprint()

    /** A workout imported earlier from [importedAs]; [nowLike] is what it looks like in the app now. */
    private fun local(id: Long, importedAs: HevyWorkout, nowLike: HevyWorkout = importedAs) = LocalSyncState(
        id = id, title = importedAs.title, startedAt = importedAs.startedAt, hevyKey = importedAs.startedAt,
        hevyFingerprint = fp(importedAs), syncedFingerprint = fp(importedAs), currentFingerprint = fp(nowLike)
    )

    @Test
    fun newWorkoutsAreAdded() {
        val plan = planSync(listOf(hevy(1), hevy(2)), emptyList(), emptySet())
        assertEquals(listOf(1L, 2L), plan.toAdd.map { it.startedAt })
        assertTrue(plan.hasChanges)
    }

    @Test
    fun untouchedOnBothSides_isUnchanged() {
        val w = hevy(1)
        val plan = planSync(listOf(w), listOf(local(10, w)), setOf(1L))
        assertEquals(1, plan.unchanged)
        assertEquals(false, plan.hasChanges)
    }

    @Test
    fun changedInHevyOnly_isUpdatedWithoutAsking() {
        val old = hevy(1, reps = 5)
        val plan = planSync(listOf(hevy(1, reps = 6)), listOf(local(10, old)), setOf(1L))
        assertEquals(listOf(10L), plan.toUpdate.map { it.localId })
        assertEquals(0, plan.conflicts.size)
    }

    @Test
    fun editedHereOnly_asks() {
        val w = hevy(1, reps = 5)
        val plan = planSync(listOf(w), listOf(local(10, w, nowLike = hevy(1, reps = 8))), setOf(1L))
        assertEquals(ConflictKind.EditedHere, plan.conflicts.single().kind)
        assertEquals(1L, plan.conflicts.single().incoming!!.startedAt)
    }

    @Test
    fun editedInBoth_asks() {
        val old = hevy(1, reps = 5)
        val plan = planSync(listOf(hevy(1, reps = 6)), listOf(local(10, old, nowLike = hevy(1, reps = 8))), setOf(1L))
        assertEquals(ConflictKind.EditedInBoth, plan.conflicts.single().kind)
        assertEquals(0, plan.toUpdate.size)
    }

    @Test
    fun deletedInHevy_isDeletedHere() {
        val plan = planSync(emptyList(), listOf(local(10, hevy(1))), setOf(1L))
        assertEquals(listOf(10L), plan.toDelete)
    }

    @Test
    fun deletedInHevyButEditedHere_asks() {
        val w = hevy(1)
        val plan = planSync(emptyList(), listOf(local(10, w, nowLike = hevy(1, reps = 9))), setOf(1L))
        assertEquals(ConflictKind.DeletedInHevy, plan.conflicts.single().kind)
        assertEquals(null, plan.conflicts.single().incoming)
        assertEquals(0, plan.toDelete.size)
    }

    @Test
    fun matchesByHevyKeyEvenAfterTheDateWasChangedHere() {
        val w = hevy(1)
        // You moved it to start at 999 in this app: still the same Hevy workout, and an edit.
        val moved = local(10, w, nowLike = hevy(1, reps = 5, title = "moved")).copy(startedAt = 999)
        val plan = planSync(listOf(w), listOf(moved), setOf(999L))
        assertEquals(0, plan.toAdd.size)
        assertEquals(0, plan.toDelete.size)
        assertEquals(ConflictKind.EditedHere, plan.conflicts.single().kind)
    }

    @Test
    fun aNewHevyWorkoutAtTheSameMinuteAsOneMadeHere_isSkipped() {
        val plan = planSync(listOf(hevy(5)), emptyList(), allStartTimes = setOf(5L))
        assertEquals(0, plan.toAdd.size)
        assertEquals(listOf(5L), plan.clashes.map { it.startedAt })
    }

    @Test
    fun twoHevyWorkoutsInTheSameMinute_keepTheFirst() {
        val plan = planSync(listOf(hevy(5, title = "first"), hevy(5, title = "second")), emptyList(), emptySet())
        assertEquals(listOf("first"), plan.toAdd.map { it.title })
        assertEquals(listOf("second"), plan.clashes.map { it.title })
    }

    @Test
    fun everyCaseInOneFile() {
        val same = hevy(1); val hevyEdit = hevy(2, reps = 5); val gone = hevy(3)
        val plan = planSync(
            file = listOf(same, hevy(2, reps = 7), hevy(4)),
            imported = listOf(local(10, same), local(20, hevyEdit), local(30, gone)),
            allStartTimes = setOf(1L, 2L, 3L)
        )
        assertEquals(1, plan.unchanged)
        assertEquals(listOf(20L), plan.toUpdate.map { it.localId })
        assertEquals(listOf(4L), plan.toAdd.map { it.startedAt })
        assertEquals(listOf(30L), plan.toDelete)
    }
}