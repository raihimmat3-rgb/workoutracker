package com.example.coach;

import com.example.coach.gemini.GeminiException;
import com.example.coach.gemini.JsonGenerator;
import com.example.coach.review.CoachService;
import com.example.coach.security.AppKeyInterceptor;
import com.example.coach.security.CoachProperties;
import com.example.coach.security.DailyLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sends real HTTP-style requests through the controller, the app-key check and the error handling,
 * without starting a server or calling Gemini (MockMvc + a fake generator).
 */
class CoachEndpointTest {

    private static final String KEY = "test-app-key";

    private static final String VALID_BODY = """
            {"workout":{"title":"Upper","date":"2026-11-16","durationMinutes":60},
             "exercises":[{"name":"Bench Press (Barbell)","sets":[{"weightKg":100,"reps":5,"type":"normal"}]}]}
            """;

    private static final String GOOD_ANSWER = """
            {"headline":"Solid session","summary":"Good work.",
             "exercises":[{"name":"Bench Press (Barbell)","trend":"new","note":"First time logged."}],
             "tips":["Add a rep next time"]}
            """;

    private MockMvc mvc(JsonGenerator generator, int dailyLimit, String serverKey) {
        CoachProperties props = new CoachProperties(serverKey, dailyLimit);
        CoachService service = new CoachService(generator, JsonMapper.builder().build(),
                new DailyLimiter(props, Clock.systemUTC()));
        return MockMvcBuilders.standaloneSetup(new CoachController(service))
                .setControllerAdvice(new ApiErrors())
                .addMappedInterceptors(new String[]{"/api/coach/**"}, new AppKeyInterceptor(props))
                .build();
    }

    private MockMvc mvc(JsonGenerator generator) {
        return mvc(generator, 10, KEY);
    }

    @Test
    void healthNeedsNoKey() throws Exception {
        mvc((s, p, schema) -> GOOD_ANSWER).perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void reviewWithTheRightKey_returnsTheReview() throws Exception {
        mvc((s, p, schema) -> GOOD_ANSWER).perform(post("/api/coach/review")
                        .header("X-App-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Solid session"))
                .andExpect(jsonPath("$.exercises[0].trend").value("new"));
    }

    @Test
    void missingOrWrongKey_is401() throws Exception {
        MockMvc mvc = mvc((s, p, schema) -> GOOD_ANSWER);
        mvc.perform(post("/api/coach/review").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("unauthorized"));
        mvc.perform(post("/api/coach/review").header("X-App-Key", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noKeyConfiguredOnTheServer_keepsTheCoachClosed() throws Exception {
        mvc((s, p, schema) -> GOOD_ANSWER, 10, "").perform(post("/api/coach/review")
                        .header("X-App-Key", "anything")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.reason").value("server_misconfigured"));
    }

    @Test
    void invalidRequests_are400WithAReason() throws Exception {
        MockMvc mvc = mvc((s, p, schema) -> GOOD_ANSWER);
        // No exercises: breaks @NotEmpty.
        mvc.perform(post("/api/coach/review").header("X-App-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workout\":{\"title\":\"x\",\"date\":\"2026-11-16\",\"durationMinutes\":1},\"exercises\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("invalid_request"));
        // Not JSON at all.
        mvc.perform(post("/api/coach/review").header("X-App-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("invalid_request"));
    }

    @Test
    void ourDailyLimit_andGeminisQuota_are429WithDifferentReasons() throws Exception {
        mvc((s, p, schema) -> GOOD_ANSWER, 0, KEY).perform(post("/api/coach/review").header("X-App-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("daily_limit"));

        mvc((s, p, schema) -> { throw new GeminiException("quota", 429); })
                .perform(post("/api/coach/review").header("X-App-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("gemini_quota"));
    }

    @Test
    void otherGeminiFailures_are502() throws Exception {
        mvc((s, p, schema) -> { throw new GeminiException("Gemini returned HTTP 500", 500); })
                .perform(post("/api/coach/review").header("X-App-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.reason").value("gemini_error"));
    }
}