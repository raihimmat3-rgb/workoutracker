package com.example.coach.gemini;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Tests the Gemini client against a FAKE server, so they run offline and use none of your quota. */
class GeminiClientTest {

    private static final String URL = "https://gemini.test/v1beta/models/test-model:generateContent";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private GeminiClient client(String key) {
        return new GeminiClient(builder, new GeminiProperties(key, "test-model", "https://gemini.test"));
    }

    private static final String ANSWER = """
            {"candidates":[{"content":{"parts":[{"text":"Hi "},{"text":"there"}]},"finishReason":"STOP"}],
             "usageMetadata":{"totalTokenCount":12}}
            """;

    @Test
    void sendsThePromptWithTheKeyAndReadsTheAnswer() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "secret-key"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("hello"))
                .andExpect(jsonPath("$.generationConfig").doesNotExist())
                .andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        assertEquals("Hi there", client("secret-key").generate("hello"));
        server.verify();
    }

    @Test
    void jsonMode_sendsTheSchemaAndTheSystemInstruction() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("be a coach"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("the data"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.type").value("OBJECT"))
                .andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        client("secret-key").generateJson("be a coach", "the data", Map.of("type", "OBJECT"));
        server.verify();
    }

    @Test
    void quotaErrorsKeepTheirStatus() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        GeminiException e = assertThrows(GeminiException.class, () -> client("secret-key").generate("hello"));
        assertEquals(429, e.status());
    }

    @Test
    void anEmptyAnswerIsAnError() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(GeminiException.class, () -> client("secret-key").generate("hello"));
    }

    @Test
    void aMissingKeyFailsBeforeCallingGemini() {
        GeminiException e = assertThrows(GeminiException.class, () -> client("").generate("hello"));
        assertEquals(0, e.status());
        server.verify(); // nothing was sent
    }
}