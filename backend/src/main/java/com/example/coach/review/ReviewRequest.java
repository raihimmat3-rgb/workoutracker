package com.example.coach.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * What the app sends to have one finished workout reviewed.
 *
 * The app does the maths: it compares with last time and puts the results in [ExerciseData.changes]
 * as plain sentences ("Set 3: 4 reps today vs 6 last time at 60 kg"). The AI only explains them.
 * AI models can get arithmetic wrong, so they never do the counting here.
 *
 * The size limits stop a huge request using up the free quota (or the server's memory).
 */
public record ReviewRequest(
        @NotNull @Valid WorkoutInfo workout,
        @NotEmpty @Size(max = 20) List<@Valid ExerciseData> exercises,
        @Valid TrainingContext context
) {
    public record WorkoutInfo(
            @NotBlank @Size(max = 80) String title,
            @NotBlank @Size(max = 20) String date,          // e.g. "2026-11-16"
            @PositiveOrZero int durationMinutes
    ) {}

    public record ExerciseData(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 40) String muscleGroup,
            @NotNull @Size(max = 20) List<@Valid SetData> sets,          // ticked sets, in order
            @Valid PreviousSession previous,                             // null = first time
            @Size(max = 10) List<@Size(max = 200) String> changes      // facts the app worked out
    ) {}

    public record SetData(Double weightKg, Integer reps, @Size(max = 20) String type) {}

    public record PreviousSession(@PositiveOrZero int daysAgo, @NotNull @Size(max = 20) List<@Valid SetData> sets) {}

    public record TrainingContext(Integer daysSinceLastWorkout, Integer workoutsThisWeek) {}
}