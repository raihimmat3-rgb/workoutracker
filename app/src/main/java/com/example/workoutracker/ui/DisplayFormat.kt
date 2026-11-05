package com.example.workoutracker.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 65 min -> "1h 5m", 60 min -> "1h", 45 min -> "45m", under a minute -> "<1m". */
fun formatDuration(millis: Long): String {
    val totalMinutes = millis.coerceAtLeast(0) / 60_000
    if (totalMinutes < 1) return "<1m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

/** 1 -> "1 set", 3 -> "3 sets". For simple words that just add an s. */
fun countLabel(count: Int, singular: String): String =
    if (count == 1) "1 $singular" else "$count ${singular}s"

/** e.g. "Mon 3 Nov 2026, 19:58", in the phone's time zone and language. */
fun formatWorkoutDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.getDefault()))