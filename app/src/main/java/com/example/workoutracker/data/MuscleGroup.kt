package com.example.workoutracker.data

/**
 * The allowed muscle groups. Keeping them in one list means "Chest", "chest" and "pecs"
 * can't all end up in the database, which keeps progress charts and filters clean later.
 * The database column is still a plain String, so adding a group here needs no migration.
 */
object MuscleGroup {
    const val CHEST = "Chest"
    const val U_BACK = "Upper Back"
    const val LATS = "Lats"
    const val SHOULDERS = "Shoulders"
    const val BICEPS = "Biceps"
    const val TRICEPS = "Triceps"
    const val FOREARMS = "Forearms"
    const val QUADS = "Quads"
    const val HAMSTRINGS = "Hamstrings"
    const val ABS = "Abs"
    const val ADDUCTORS = "Adductors"
    const val ABDUCTORS = "Abductors"
    const val CALVES = "Calves"
    const val OTHER = "Other"

    val options = listOf(CHEST, U_BACK, LATS, SHOULDERS, BICEPS, TRICEPS, FOREARMS, HAMSTRINGS, QUADS, ABS, ADDUCTORS, ABDUCTORS, CALVES ,OTHER)
}