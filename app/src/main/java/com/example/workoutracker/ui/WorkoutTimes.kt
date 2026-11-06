package com.example.workoutracker.ui

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Moves a workout to [newStartedAt] while keeping its duration:
 * the end moves by exactly the same amount as the start. Returns (newStart, newEnd).
 */
fun shiftWorkoutTimes(startedAt: Long, endedAt: Long, newStartedAt: Long): Pair<Long, Long> =
    newStartedAt to endedAt + (newStartedAt - startedAt)

/**
 * Material's DatePicker works in "UTC midnight of the chosen day". This turns a real moment
 * into that format for the day it falls on in [zone], so the picker opens on the right date.
 */
fun toPickerDateMillis(epochMillis: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(epochMillis)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

/** Combines the DatePicker's day (UTC midnight) with an hour and minute in [zone] into a real moment. */
fun combineDateAndTime(pickerDateMillis: Long, hour: Int, minute: Int, zone: ZoneId): Long =
    Instant.ofEpochMilli(pickerDateMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .atTime(hour, minute)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()