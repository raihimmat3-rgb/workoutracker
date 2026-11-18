package com.example.coach.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings from application.properties (which reads them from environment variables).
 *
 * @param apiKey  your key from Google AI Studio. Never written in code or committed.
 * @param model   the Gemini model id, e.g. "gemini-flash-latest". Copy the exact id from AI Studio.
 * @param baseUrl Google's API address. Only changed in tests.
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String model, String baseUrl) {
}
