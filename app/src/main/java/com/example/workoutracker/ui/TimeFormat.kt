package com.example.workoutracker.ui

import java.util.Locale

/** 307 s -> "05:07", 3907 s -> "1:05:07". Negative values are treated as zero. */
fun formatElapsed(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(Locale.ROOT, hours, minutes, seconds)
    } else {
        "%02d:%02d".format(Locale.ROOT, minutes, seconds)
    }
}

/** A friendly default title based on the hour of day (0-23) the workout started. */
fun defaultWorkoutTitle(hourOfDay: Int): String = when (hourOfDay) {
    in 5..11 -> "Morning workout"
    in 12..16 -> "Afternoon workout"
    in 17..21 -> "Evening workout"
    else -> "Night workout"
}

/** True if a workout is too short to be a real session (probably started by accident). */
fun isSuspiciouslyShort(elapsedMillis: Long): Boolean = elapsedMillis < 60_000