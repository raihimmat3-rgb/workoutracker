package com.example.workoutracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Milliseconds since [startedAt], refreshed every second while on screen.
 * The time is computed from the saved start timestamp, never counted up,
 * so it stays correct after the app is backgrounded or restarted.
 */
@Composable
fun rememberElapsedMillis(startedAt: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return now - startedAt
}