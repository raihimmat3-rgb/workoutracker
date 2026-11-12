package com.example.workoutracker.hevy

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Turns a Hevy "Export workouts" CSV into [HevyWorkout]s.
 *
 * Each CSV row is ONE SET, with its workout's and exercise's details repeated on every row.
 * The parser groups rows back into workouts (by start time) and, within each workout,
 * consecutive rows with the same exercise title into one exercise block.
 */
class HevyCsvParser(private val zone: ZoneId = ZoneId.systemDefault()) {

    // e.g. "16 Oct 2026, 22:07" or "9 Oct 2026, 22:20".
    // Locale.ENGLISH on purpose: with Locale.UK, newer Java and Android spell September "Sept",
    // so "Sep" in your file would fail to parse. Case-insensitive just in case.
    private val dateFormat: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMM yyyy, HH:mm")
        .toFormatter(Locale.ENGLISH)

    fun parse(text: String): HevyParseResult {
        val records = readCsv(text)
        if (records.isEmpty()) throw HevyFormatException("The file is empty")

        val header = records.first().second.map { it.trim().lowercase() }
        val col = Columns(header)
        val problems = mutableListOf<RowProblem>()

        // Read every row into a flat list first, skipping the ones that can't be read.
        val rows = records.drop(1).mapNotNull { (line, fields) ->
            try {
                col.readRow(fields, line)
            } catch (e: RowException) {
                problems.add(RowProblem(line, e.message ?: "Couldn't read this row"))
                null
            }
        }

        // Group into workouts. LinkedHashMap keeps the file's order (Hevy lists newest first).
        val byWorkout = LinkedHashMap<Long, MutableList<Row>>()
        rows.forEach { byWorkout.getOrPut(it.startedAt) { mutableListOf() }.add(it) }

        val workouts = byWorkout.values.map { workoutRows ->
            val first = workoutRows.first()
            HevyWorkout(
                title = first.title,
                startedAt = first.startedAt,
                endedAt = first.endedAt,
                description = first.description,
                exercises = groupExercises(workoutRows)
            )
        }
        return HevyParseResult(workouts, problems)
    }

    /** Consecutive rows with the same exercise title are one block; sets sorted by set_index. */
    private fun groupExercises(rows: List<Row>): List<HevyExercise> {
        val blocks = mutableListOf<MutableList<Row>>()
        rows.forEach { row ->
            val current = blocks.lastOrNull()
            if (current != null && current.first().exerciseTitle == row.exerciseTitle) current.add(row)
            else blocks.add(mutableListOf(row))
        }
        return blocks.map { block ->
            HevyExercise(
                title = block.first().exerciseTitle,
                notes = block.first().exerciseNotes,
                sets = block.sortedBy { it.set.index }.map { it.set }
            )
        }
    }

    // ---- reading one row ----

    private class RowException(message: String) : Exception(message)

    private data class Row(
        val title: String,
        val startedAt: Long,
        val endedAt: Long,
        val description: String?,
        val exerciseTitle: String,
        val exerciseNotes: String?,
        val set: HevySet
    )

    /** Finds each column by its header name, so the parser doesn't depend on column order. */
    private inner class Columns(header: List<String>) {
        private val index = header.withIndex().associate { (i, name) -> name to i }

        private fun required(name: String): Int =
            index[name] ?: throw HevyFormatException(
                "This doesn't look like a Hevy workout export: the \"$name\" column is missing"
            )

        private val title = required("title")
        private val start = required("start_time")
        private val end = required("end_time")
        private val exercise = required("exercise_title")
        private val setIndex = required("set_index")
        private val description = index["description"]
        private val notes = index["exercise_notes"]
        private val setType = index["set_type"]
        // Hevy exports weight_kg, or weight_lbs if your account uses pounds. We always store kg.
        private val weightKg = index["weight_kg"]
        private val weightLbs = index["weight_lbs"]
        private val reps = index["reps"]
        private val distanceMiles = index["distance_miles"]
        private val distanceKm = index["distance_km"]
        private val duration = index["duration_seconds"]
        private val rpe = index["rpe"]

        fun readRow(fields: List<String>, line: Int): Row {
            fun get(i: Int?): String? = i?.let { fields.getOrNull(it) }?.trim()?.ifEmpty { null }
            fun need(i: Int, name: String): String = get(i) ?: throw RowException("Missing $name")
            fun double(i: Int?, name: String): Double? =
                get(i)?.let { it.toDoubleOrNull() ?: throw RowException("$name isn't a number: \"$it\"") }
            fun int(i: Int?, name: String): Int? =
                get(i)?.let { v ->
                    v.toIntOrNull() ?: v.toDoubleOrNull()?.toInt() ?: throw RowException("$name isn't a whole number: \"$v\"")
                }

            val startedAt = parseTime(need(start, "start_time"))
            val endedAt = get(end)?.let(::parseTime) ?: startedAt
            val kg = double(weightKg, "weight_kg") ?: double(weightLbs, "weight_lbs")?.let { it * KG_PER_LB }
            val meters = double(distanceKm, "distance_km")?.let { it * 1000 }
                ?: double(distanceMiles, "distance_miles")?.let { it * METERS_PER_MILE }

            return Row(
                title = need(title, "title"),
                startedAt = startedAt,
                endedAt = endedAt,
                description = get(description),
                exerciseTitle = need(exercise, "exercise_title"),
                exerciseNotes = get(notes),
                set = HevySet(
                    index = int(setIndex, "set_index") ?: throw RowException("Missing set_index"),
                    type = get(setType)?.lowercase() ?: "normal",
                    weightKg = kg,
                    reps = int(reps, "reps"),
                    distanceMeters = meters,
                    durationSeconds = int(duration, "duration_seconds"),
                    rpe = double(rpe, "rpe")
                )
            )
        }

        private fun parseTime(text: String): Long =
            try {
                LocalDateTime.parse(text, dateFormat).atZone(zone).toInstant().toEpochMilli()
            } catch (e: DateTimeParseException) {
                throw RowException("Couldn't read the date \"$text\"")
            }
    }

    private companion object {
        const val KG_PER_LB = 0.45359237
        const val METERS_PER_MILE = 1609.344
    }
}