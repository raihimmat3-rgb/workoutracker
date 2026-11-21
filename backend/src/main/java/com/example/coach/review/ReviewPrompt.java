package com.example.coach.review;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/** The instructions, the data and the answer format for a workout review. */
public final class ReviewPrompt {

    private ReviewPrompt() {}

    /** The coach's role and rules. Kept apart from the data, so data can't override them. */
    public static final String SYSTEM_INSTRUCTION = """
            You are a strength coach reviewing ONE finished gym workout for the lifter who did it.

            Rules:
            - Use ONLY the numbers in the data. Never invent sets, weights, reps, dates or exercises.
            - The "changes" lines are facts the app already calculated. Explain them; don't recalculate them.
            - When reps dropped, give the most likely reasons the data supports, for example: it was a
              later set in the session, the weight went up since last time, few days since the previous
              session, or many hard sets before it. If the data can't explain it, say so plainly:
              sleep, food, stress and rest times between sets are NOT recorded.
            - Be specific, honest and encouraging. Plain British English, no jargon without a short explanation.
            - headline: under 12 words. summary: 2-3 sentences. Each exercise note: 1-2 sentences.
            - trend per exercise: "up" (more weight or reps than last time), "same", "down", or "new" (no previous session).
            - tips: at most 3 short, practical next steps for the next session.
            - This is general training information, not medical advice. If anything suggests pain or
              injury, suggest seeing a qualified professional.
            """;

    /** The data as compact JSON. Models read JSON well, and it keeps every number exact. */
    public static String build(ReviewRequest request, ObjectMapper mapper) {
        return "Review this workout. Data (JSON):\n" + mapper.writeValueAsString(request);
    }

    /** The shape Gemini must answer in (its structured-output schema format). */
    public static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "headline", Map.of("type", "STRING"),
                    "summary", Map.of("type", "STRING"),
                    "exercises", Map.of(
                            "type", "ARRAY",
                            "items", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "name", Map.of("type", "STRING"),
                                            "trend", Map.of("type", "STRING", "enum", List.of("up", "same", "down", "new")),
                                            "note", Map.of("type", "STRING")
                                    ),
                                    "required", List.of("name", "trend", "note"),
                                    "propertyOrdering", List.of("name", "trend", "note")
                            )
                    ),
                    "tips", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
            ),
            "required", List.of("headline", "summary", "exercises", "tips"),
            "propertyOrdering", List.of("headline", "summary", "exercises", "tips")
    );
}