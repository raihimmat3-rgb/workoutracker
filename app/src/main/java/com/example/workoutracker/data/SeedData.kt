package com.example.workoutracker.data

import java.util.concurrent.TimeUnit

/**
 * TEMPORARY dev helper: inserts three sample workouts so the history screen has something to show.
 * You'll delete this once real logging (week 2) and the Hevy import (week 4) exist.
 */
suspend fun seedSampleData(db: AppDatabase) {
    val exerciseDao = db.exerciseDao()
    val workoutDao = db.workoutDao()

    suspend fun exerciseId(name: String): Long {
        val inserted = exerciseDao.insert(ExerciseEntity(name = name))
        return if (inserted != -1L) inserted else exerciseDao.findByName(name)!!.id
    }

    suspend fun addWorkout(
        title: String,
        daysAgo: Long,
        exercises: List<Pair<String, List<Pair<Double, Int>>>>
    ) {
        val start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysAgo)
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(
                title = title,
                startedAt = start,
                endedAt = start + TimeUnit.MINUTES.toMillis(65),
                source = "sample"
            )
        )
        if (workoutId == -1L) return // same start time already exists

        exercises.forEachIndexed { position, (name, sets) ->
            val weId = workoutDao.insertWorkoutExercise(
                WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId(name), position = position)
            )
            workoutDao.insertSets(
                sets.mapIndexed { i, (kg, reps) ->
                    WorkoutSetEntity(workoutExerciseId = weId, setIndex = i, weightKg = kg, reps = reps)
                }
            )
        }
    }

    addWorkout(
        "Upper", daysAgo = 1,
        exercises = listOf(
            "Tricep Pushdown" to listOf(25.0 to 7, 25.0 to 8),
            "Dumbbell Curl" to listOf(18.0 to 7, 18.0 to 9)
        )
    )
    addWorkout(
        "Lower", daysAgo = 3,
        exercises = listOf(
            "Squat" to listOf(100.0 to 5, 100.0 to 5, 100.0 to 4),
            "Leg Curl" to listOf(40.0 to 10, 40.0 to 9)
        )
    )
    addWorkout(
        "Pull", daysAgo = 5,
        exercises = listOf(
            "Lat Pulldown" to listOf(55.0 to 10, 55.0 to 9),
            "Barbell Row" to listOf(60.0 to 8, 60.0 to 8)
        )
    )
}