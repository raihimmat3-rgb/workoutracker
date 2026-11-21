package com.example.coach.review;

import com.example.coach.gemini.GeminiException;
import com.example.coach.gemini.JsonGenerator;
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

    public CoachService(JsonGenerator generator, ObjectMapper mapper) {
        this.generator = generator;
        this.mapper = mapper;
    }

    public CoachReview review(ReviewRequest request) {
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