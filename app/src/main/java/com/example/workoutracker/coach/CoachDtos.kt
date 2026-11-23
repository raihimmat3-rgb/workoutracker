package com.example.workoutracker.coach

import kotlinx.serialization.Serializable

/*
 * The JSON the app and the coach server exchange. These must match the Java records on the
 * server (ReviewRequest and CoachReview): same field names, same nesting.
 * @Serializable lets kotlinx.serialization turn them into JSON and back.
 */

@Serializable
data class ReviewRequestDto(
    val workout: WorkoutInfoDto,
    val exercises: List<ExerciseDataDto>,
    val context: TrainingContextDto? = null
)

@Serializable
data class WorkoutInfoDto(val title: String, val date: String, val durationMinutes: Int)

@Serializable
data class ExerciseDataDto(
    val name: String,
    val muscleGroup: String? = null,
    val sets: List<SetDataDto>,
    val previous: PreviousSessionDto? = null,
    /** Facts the app worked out ("Set 3: 3 reps today vs 5 last time"). The AI only explains them. */
    val changes: List<String> = emptyList()
)

@Serializable
data class SetDataDto(val weightKg: Double? = null, val reps: Int? = null, val type: String? = null)

@Serializable
data class PreviousSessionDto(val daysAgo: Int, val sets: List<SetDataDto>)

@Serializable
data class TrainingContextDto(val daysSinceLastWorkout: Int? = null, val workoutsThisWeek: Int? = null)

@Serializable
data class CoachReviewDto(
    val headline: String,
    val summary: String,
    val exercises: List<ExerciseNoteDto> = emptyList(),
    val tips: List<String> = emptyList()
)

@Serializable
data class ExerciseNoteDto(val name: String, val trend: String, val note: String)

/** The server's error body: {"reason": "...", "error": "..."}. */
@Serializable
data class ApiErrorDto(val reason: String? = null, val error: String? = null)