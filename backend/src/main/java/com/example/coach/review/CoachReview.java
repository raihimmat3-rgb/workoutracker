package com.example.coach.review;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Set;

/** The coach's review of one workout: what the app shows on the Coach card. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CoachReview(
        String headline,                // e.g. "Strong upper day, bench is moving"
        String summary,                 // 2-3 sentences on the whole session
        List<ExerciseNote> exercises,   // one per exercise the coach had something to say about
        List<String> tips               // at most 3 next steps
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExerciseNote(String name, String trend, String note) {}

    private static final Set<String> TRENDS = Set.of("up", "same", "down", "new");
    static final int MAX_TIPS = 3;

    /**
     * Cleans up what the model sent, without trusting it: drops notes about exercises that weren't
     * in the workout (a sign it made something up), drops empty notes, fixes unknown trends, and
     * keeps at most [MAX_TIPS] tips. Never returns nulls, so the app doesn't have to check.
     */
    public CoachReview tidy(ReviewRequest request) {
        Set<String> realNames = new java.util.HashSet<>();
        request.exercises().forEach(e -> realNames.add(e.name().trim().toLowerCase()));

        List<ExerciseNote> cleanNotes = exercises == null ? List.of() : exercises.stream()
                .filter(n -> n != null && n.name() != null && n.note() != null && !n.note().isBlank())
                .filter(n -> realNames.contains(n.name().trim().toLowerCase()))
                .map(n -> new ExerciseNote(n.name().trim(), TRENDS.contains(n.trend()) ? n.trend() : "same", n.note().trim()))
                .toList();
        List<String> cleanTips = tips == null ? List.of() : tips.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .limit(MAX_TIPS)
                .toList();
        return new CoachReview(
                headline == null || headline.isBlank() ? "Workout reviewed" : headline.trim(),
                summary == null ? "" : summary.trim(),
                cleanNotes,
                cleanTips
        );
    }
}