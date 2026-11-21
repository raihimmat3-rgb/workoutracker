package com.example.coach.review;

import com.example.coach.gemini.GeminiException;
import com.example.coach.gemini.JsonGenerator;
import com.example.coach.security.DailyLimitExceededException;
import com.example.coach.security.DailyLimiter;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns a workout into a review: build the prompt, ask the model for JSON, read it, tidy it.
 * (Spring Boot 4 uses Jackson 3, whose classes live under "tools.jackson".)
 */
@Service
public class CoachService {

    private final JsonGenerator generator;
    private final ObjectMapper mapper;
    private final DailyLimiter limiter;

    public CoachService(JsonGenerator generator, ObjectMapper mapper, DailyLimiter limiter) {
        this.generator = generator;
        this.mapper = mapper;
        this.limiter = limiter;
    }

    public CoachReview review(ReviewRequest request) {
        // Checked before calling Gemini, so a refused request costs nothing.
        if (!limiter.tryAcquire()) throw new DailyLimitExceededException();
        String json = generator.generateJson(
                ReviewPrompt.SYSTEM_INSTRUCTION,
                ReviewPrompt.build(request, mapper),
                ReviewPrompt.RESPONSE_SCHEMA
        );
        CoachReview raw;
        try {
            raw = mapper.readValue(json, CoachReview.class);
        } catch (JacksonException e) {
            throw new GeminiException("The coach's answer wasn't in the expected format", 0, e);
        }
        return raw.tidy(request);
    }
}