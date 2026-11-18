package com.example.coach.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Calls Gemini's REST API directly with Spring's RestClient (no Google SDK needed):
 * POST {baseUrl}/v1beta/models/{model}:generateContent, with the key in the x-goog-api-key header.
 */
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiClient(RestClient.Builder builder, GeminiProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    /** Sends one prompt and returns Gemini's text answer. */
    public String generate(String prompt) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new GeminiException("GEMINI_API_KEY is not set on the server", 0);
        }
        var request = new GenerateRequest(List.of(new Content(List.of(new Part(prompt)))));
        try {
            GenerateResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GenerateResponse.class);
            return extractText(response);
        } catch (RestClientResponseException e) {
            // e.g. 400 = bad request or unknown model, 403 = bad key, 429 = free quota used up.
            int status = e.getStatusCode().value();
            throw new GeminiException("Gemini returned HTTP " + status, status, e);
        } catch (RestClientException e) {
            throw new GeminiException("Couldn't reach Gemini: " + e.getMessage(), 0, e);
        }
    }

    /** Pulls the text out of Gemini's nested reply: candidates[0].content.parts[*].text. */
    static String extractText(GenerateResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new GeminiException("Gemini sent no answer", 0);
        }
        Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null) {
            throw new GeminiException("Gemini's answer was empty", 0);
        }
        StringBuilder text = new StringBuilder();
        for (Part part : content.parts()) {
            if (part.text() != null) text.append(part.text());
        }
        if (text.isEmpty()) throw new GeminiException("Gemini's answer had no text", 0);
        return text.toString().trim();
    }

    // ---- The JSON shapes Gemini expects and returns (only the fields we use) ----

    record GenerateRequest(List<Content> contents) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {}
}
