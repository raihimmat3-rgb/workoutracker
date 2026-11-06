package com.example.workoutracker.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.ZoneId

/**
 * Two steps: pick a day, then a start time. Calls [onPicked] with the new start (epoch millis).
 * Days after today can't be picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDateTimePicker(
    startedAt: Long,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    // null while on the date step; the chosen day (UTC midnight) once on the time step.
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val day = pickedDay

    if (day == null) {
        val today = remember { toPickerDateMillis(System.currentTimeMillis(), zone) }
        val notInFuture = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            }
        }
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = toPickerDateMillis(startedAt, zone),
            selectableDates = notInFuture
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { pickedDay = dateState.selectedDateMillis },
                    enabled = dateState.selectedDateMillis != null
                ) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val current = remember(startedAt) { Instant.ofEpochMilli(startedAt).atZone(zone) }
        val timeState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Start time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onPicked(combineDateAndTime(day, timeState.hour, timeState.minute, zone))
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickedDay = null }) { Text("Back") } }
        )
    }
}