package com.example.workoutracker.data

data class WorkoutSummary(
    val id: Long,
    val title: String,
    val startedAt: Long,
    val endedAt: Long?,
    val source: String,
    val exerciseCount: Int,
    val doneSets: Int,
    val volumeKg: Double
)