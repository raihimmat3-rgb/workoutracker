package com.example.workoutracker.coach

import com.example.workoutracker.data.ExerciseEntity
import com.example.workoutracker.data.WorkoutEntity
import com.example.workoutracker.data.WorkoutExerciseEntity
import com.example.workoutracker.data.WorkoutExerciseWithSets
import com.example.workoutracker.data.WorkoutSetEntity
import com.example.workoutracker.data.WorkoutWithDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CoachFactsTest {

    private fun s(kg: Double?, reps: Int?) = SetValues(kg, reps)

    // ---- describeChanges ----

    @Test
    fun firstTime_saysSo() {
        assertEquals(listOf("First time logging this exercise"), describeChanges(listOf(s(60.0, 8)), null))
    }

    @Test
    fun heavierWithARepDropOnTheLastSet() {
        val lines = describeChanges(
            today = listOf(s(100.0, 5), s(100.0, 5), s(100.0, 3)),
            previous = listOf(s(97.5, 5), s(97.5, 5), s(97.5, 5))
        )
        assertEquals("Top weight up from 97.5 kg to 100 kg", lines[0])
        assertTrue(lines.contains("Set 3: 100 kg × 3 today vs 97.5 kg × 5 last time"))
        assertTrue(lines.contains("Within today: reps fell from 5 on set 1 to 3 on set 3 at 100 kg"))
    }

    @Test
    fun sameWeight_fewerReps() {
        val lines = describeChanges(
            today = listOf(s(60.0, 8), s(60.0, 6)),
            previous = listOf(s(60.0, 8), s(60.0, 8))
        )
        assertEquals(listOf("Set 2: 6 reps today vs 8 last time at 60 kg", "Within today: reps fell from 8 on set 1 to 6 on set 2 at 60 kg"), lines)
    }

    @Test
    fun moreSetsAndBodyweight() {
        val lines = describeChanges(
            today = listOf(s(null, 12), s(null, 12), s(null, 10)),
            previous = listOf(s(null, 10), s(null, 10))
        )
        assertEquals(
            listOf(
                "3 sets today vs 2 last time",
                "Set 1: 12 reps today vs 10 last time",
                "Set 2: 12 reps today vs 10 last time",
                "Within today: reps fell from 12 on set 1 to 10 on set 3" // no weight: bodyweight
            ),
            lines
        )
    }

    @Test
    fun noChange_noLines() {
        assertEquals(emptyList<String>(), describeChanges(listOf(s(60.0, 8)), listOf(s(60.0, 8))))
    }

    @Test
    fun smallFadesAreIgnored_andLinesAreCapped() {
        assertEquals(emptyList<String>(), describeChanges(listOf(s(60.0, 8), s(60.0, 7)), listOf(s(60.0, 8), s(60.0, 7))))
        val many = (1..15).map { s(50.0 + it, 5) }
        assertEquals(MAX_CHANGE_LINES, describeChanges(many, many.map { s(it.weightKg!! - 1, 5) }).size)
    }

    // ---- buildReviewRequest ----

    private val london = ZoneId.of("Europe/London")
    private fun at(d: Int, h: Int) = LocalDateTime.of(2026, 11, d, h, 0).atZone(london).toInstant().toEpochMilli()

    private fun set(kg: Double, reps: Int, done: Boolean = true, index: Int = 0) =
        WorkoutSetEntity(workoutExerciseId = 1, setIndex = index, weightKg = kg, reps = reps, isDone = done)

    private fun workout(vararg exercises: WorkoutExerciseWithSets) = WorkoutWithDetails(
        WorkoutEntity(id = 1, title = "Upper", startedAt = at(16, 18), endedAt = at(16, 18) + 72 * 60_000),
        exercises.toList()
    )

    private fun item(id: Long, name: String, position: Int, vararg sets: WorkoutSetEntity) = WorkoutExerciseWithSets(
        WorkoutExerciseEntity(id = id, workoutId = 1, exerciseId = id, position = position),
        sets.toList(),
        ExerciseEntity(id = id, name = name, muscleGroup = "Chest")
    )

    @Test
    fun buildsTheRequestFromTickedSetsOnly() {
        val details = workout(
            item(2, "Fly", 1, set(15.0, 12, done = false)),                  // nothing ticked: left out
            item(1, "Bench", 0, set(100.0, 5, index = 0), set(100.0, 3, index = 1), set(120.0, 1, done = false, index = 2))
        )
        val previous = mapOf(1L to PreviousData(at(12, 18), listOf(set(97.5, 5), set(97.5, 5))))

        val request = buildReviewRequest(details, previous, daysSinceLastWorkout = 2, workoutsThisWeek = 3, zone = london)!!

        assertEquals("2026-11-16", request.workout.date)
        assertEquals(72, request.workout.durationMinutes)
        assertEquals(listOf("Bench"), request.exercises.map { it.name })
        val bench = request.exercises.single()
        assertEquals(listOf(5, 3), bench.sets.map { it.reps })
        assertEquals(4, bench.previous!!.daysAgo)
        assertTrue(bench.changes.first().startsWith("Top weight up"))
        assertEquals(2, request.context!!.daysSinceLastWorkout)
    }

    @Test
    fun nothingTicked_meansNoRequest() {
        assertNull(buildReviewRequest(workout(item(1, "Bench", 0, set(100.0, 5, done = false))), emptyMap(), null, null, london))
    }

    @Test
    fun daysBetween_countsCalendarDays() {
        assertEquals(1, daysBetween(at(15, 22), at(16, 7), london))
        assertEquals(0, daysBetween(at(16, 7), at(16, 22), london))
    }
}