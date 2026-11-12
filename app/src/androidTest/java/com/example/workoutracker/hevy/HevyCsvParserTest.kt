package com.example.workoutracker.hevy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.collections.get

/**
 * The sample rows below copy the exact FORMAT of your real export (same columns, quoting and
 * date style) but with made-up workouts, so none of your own training data goes into GitHub.
 */
class HevyCsvParserTest {

    private val london = ZoneId.of("Europe/London")
    private val parser = HevyCsvParser(london)
    private fun london(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(london).toInstant().toEpochMilli()

    private val header =
        "\"title\",\"start_time\",\"end_time\",\"description\",\"exercise_title\",\"superset_id\"," +
                "\"exercise_notes\",\"set_index\",\"set_type\",\"weight_kg\",\"reps\",\"distance_miles\"," +
                "\"duration_seconds\",\"rpe\""

    private fun csv(vararg rows: String) = (listOf(header) + rows).joinToString("\n") + "\n"

    private val twoWorkouts = csv(
        // Newest first, like Hevy. Workout 1: two exercises, bench has two sets.
        "\"push, heavy\",\"16 Oct 2026, 22:07\",\"16 Oct 2026, 23:37\",\"felt strong\",\"Bench Press (Barbell)\",,\"\",0,\"normal\",100,5,,,",
        "\"push, heavy\",\"16 Oct 2026, 22:07\",\"16 Oct 2026, 23:37\",\"felt strong\",\"Bench Press (Barbell)\",,\"\",1,\"normal\",97.5,6,,,",
        "\"push, heavy\",\"16 Oct 2026, 22:07\",\"16 Oct 2026, 23:37\",\"felt strong\",\"Hanging Leg Raise\",,\"slow\",0,\"normal\",,12,,,",
        // Workout 2: a single-digit day and September ("Sep").
        "\"legs\",\"9 Sep 2026, 07:05\",\"9 Sep 2026, 08:10\",\"\",\"Squat (Barbell)\",,\"\",0,\"warmup\",60,10,,,"
    )

    @Test
    fun groupsRowsIntoWorkoutsExercisesAndSets() {
        val result = parser.parse(twoWorkouts)

        assertEquals(0, result.problems.size)
        assertEquals(listOf("push, heavy", "legs"), result.workouts.map { it.title })

        val push = result.workouts[0]
        assertEquals("felt strong", push.description)
        assertEquals(listOf("Bench Press (Barbell)", "Hanging Leg Raise"), push.exercises.map { it.title })
        assertEquals(listOf(5, 6), push.exercises[0].sets.map { it.reps })
        assertEquals(97.5, push.exercises[0].sets[1].weightKg!!, 0.0001)
    }

    @Test
    fun readsDatesInTheGivenTimeZone() {
        val push = parser.parse(twoWorkouts).workouts[0]
        assertEquals(london(2026, 10, 16, 22, 7), push.startedAt)
        assertEquals(london(2026, 10, 16, 23, 37), push.endedAt)
    }

    @Test
    fun readsSingleDigitDaysAndSeptember() {
        val legs = parser.parse(twoWorkouts).workouts[1]
        assertEquals(london(2026, 9, 9, 7, 5), legs.startedAt)
    }

    @Test
    fun emptyCellsBecomeNull() {
        val push = parser.parse(twoWorkouts).workouts[0]
        val legRaise = push.exercises[1]
        assertNull(legRaise.sets[0].weightKg)            // bodyweight
        assertEquals("slow", legRaise.notes)
        assertNull(push.exercises[0].notes)              // "" in the file
        assertNull(parser.parse(twoWorkouts).workouts[1].description)
    }

    @Test
    fun keepsSetType() {
        assertEquals("warmup", parser.parse(twoWorkouts).workouts[1].exercises[0].sets[0].type)
    }

    @Test
    fun theSameExerciseLaterInTheWorkoutIsASeparateBlock() {
        val result = parser.parse(
            csv(
                "\"w\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 11:00\",\"\",\"Curl\",,\"\",0,\"normal\",20,10,,,",
                "\"w\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 11:00\",\"\",\"Row\",,\"\",0,\"normal\",50,10,,,",
                "\"w\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 11:00\",\"\",\"Curl\",,\"\",0,\"normal\",18,12,,,"
            )
        )
        assertEquals(listOf("Curl", "Row", "Curl"), result.workouts.single().exercises.map { it.title })
    }

    @Test
    fun badRowsAreSkippedAndReportedWithTheirLine() {
        val result = parser.parse(
            csv(
                "\"w\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 11:00\",\"\",\"Curl\",,\"\",0,\"normal\",20,10,,,",
                "\"w\",\"not a date\",\"1 Jan 2026, 11:00\",\"\",\"Curl\",,\"\",1,\"normal\",20,10,,,",
                "\"w\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 11:00\",\"\",\"Curl\",,\"\",2,\"normal\",heavy,10,,,"
            )
        )
        assertEquals(1, result.workouts.single().exercises.single().sets.size)
        assertEquals(listOf(3, 4), result.problems.map { it.line })
        assertTrue(result.problems[0].message.contains("date"))
    }

    @Test
    fun convertsPoundsAndMilesToKgAndMetres() {
        // If your Hevy account used pounds and miles, the columns are named differently.
        val header = "title,start_time,end_time,exercise_title,set_index,weight_lbs,reps,distance_miles"
        val result = parser.parse(
            "$header\n" +
                    "w,\"1 Jan 2026, 10:00\",\"1 Jan 2026, 11:00\",Curl,0,100,10,\n" +
                    "w,\"1 Jan 2026, 10:00\",\"1 Jan 2026, 11:00\",Run,0,,,1\n"
        )
        val exercises = result.workouts.single().exercises
        assertEquals(45.359237, exercises[0].sets.single().weightKg!!, 0.0001)
        assertEquals(1609.344, exercises[1].sets.single().distanceMeters!!, 0.001)
    }

    @Test(expected = HevyFormatException::class)
    fun aFileWithoutTheRightColumnsIsRejected() {
        parser.parse("name,price\nphone,100\n")
    }

    @Test(expected = HevyFormatException::class)
    fun anEmptyFileIsRejected() {
        parser.parse("")
    }
}