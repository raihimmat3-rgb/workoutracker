package com.example.coach.gemini;

import java.util.Map;

/**
 * "Something that turns a prompt into JSON matching a schema." GeminiClient is the real one;
 * tests pass a fake, so they never call Google or use your quota. Depending on this small
 * interface instead of on GeminiClient directly is what makes that swap possible.
 */
@FunctionalInterface
public interface JsonGenerator {
    String generateJson(String systemInstruction, String prompt, Map<String, Object> schema);
}