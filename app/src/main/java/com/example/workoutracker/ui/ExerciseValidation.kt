package com.example.workoutracker.ui

/** Returns an error message to show the user, or null if the name is acceptable. */
fun validateExerciseName(name: String): String? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> "Enter a name"
        trimmed.length > 60 -> "Name is too long (max 60 characters)"
        else -> null
    }
}