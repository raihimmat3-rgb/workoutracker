package com.example.coach.gemini;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests the Gemini client against a FAKE server, so they run offline and use none of your quota.
 */
class GeminiClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private GeminiClient client(String key) {
        return new GeminiClient(builder, new GeminiProperties(key, "test-model", "https://gemini.test"));
    }

    @Test
    void sendsThePromptWithTheKeyAndReadsTheAnswer() {
        GeminiClient client = client("secret-key");
        server.expect(requestTo("https://gemini.test/v1beta/models/test-model:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "secret-key"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("hello"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"Hi "},{"text":"there"}]},
                                        "finishReason":"STOP"}],
                         "usageMetadata":{"totalTokenCount":12}}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("Hi there", client.generate("hello"));
        server.verify();
    }

    @Test
    void quotaErrorsKeepTheirStatus() {
        GeminiClient client = client("secret-key");
        server.expect(requestTo("https://gemini.test/v1beta/models/test-model:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        GeminiException e = assertThrows(GeminiException.class, () -> client.generate("hello"));
        assertEquals(429, e.status());
    }

    @Test
    void anEmptyAnswerIsAnError() {
        GeminiClient client = client("secret-key");
        server.expect(requestTo("https://gemini.test/v1beta/models/test-model:generateContent"))
                .andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(GeminiException.class, () -> client.generate("hello"));
    }

    @Test
    void aMissingKeyFailsBeforeCallingGemini() {
        GeminiException e = assertThrows(GeminiException.class, () -> client("").generate("hello"));
        assertEquals(0, e.status());
        server.verify(); // nothing was sent
    }
}
