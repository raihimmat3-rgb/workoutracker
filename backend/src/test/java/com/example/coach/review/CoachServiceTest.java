package com.example.coach.review;

import com.example.coach.gemini.GeminiException;
import com.example.coach.review.ReviewRequest.ExerciseData;
import com.example.coach.review.ReviewRequest.SetData;
import com.example.coach.review.ReviewRequest.WorkoutInfo;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses a FAKE generator (a lambda), so these never call Gemini. */
class CoachServiceTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    private final ReviewRequest request = new ReviewRequest(
            new WorkoutInfo("Upper", "2026-11-16", 72),
            List.of(new ExerciseData(
                    "Bench Press (Barbell)", "Chest",
                    List.of(new SetData(100.0, 5, "normal"), new SetData(100.0, 3, "normal")),
                    null,
                    List.of("Set 2: 3 reps today vs 5 last time")
            )),
            null
    );

    @Test
    void sendsTheRulesTheDataAndTheSchema_andReadsTheAnswer() {
        AtomicReference<String> sentSystem = new AtomicReference<>();
        AtomicReference<String> sentPrompt = new AtomicReference<>();
        AtomicReference<Object> sentSchema = new AtomicReference<>();
        CoachService service = new CoachService((system, prompt, schema) -> {
            sentSystem.set(system);
            sentPrompt.set(prompt);
            sentSchema.set(schema);
            return """
                    {"headline":"Heavier bench, last set dipped",
                     "summary":"You added 2.5 kg.",
                     "exercises":[{"name":"Bench Press (Barbell)","trend":"up","note":"Weight went up."}],
                     "tips":["Rest a little longer before the last set"]}
                    """;
        }, mapper);

        CoachReview review = service.review(request);

        assertEquals("Heavier bench, last set dipped", review.headline());
        assertEquals("up", review.exercises().get(0).trend());
        assertTrue(sentSystem.get().contains("Use ONLY the numbers"));
        assertTrue(sentPrompt.get().contains("Set 2: 3 reps today vs 5 last time"));
        assertSame(ReviewPrompt.RESPONSE_SCHEMA, sentSchema.get());
    }

    @Test
    void notesAboutExercisesThatWerentDone_areDropped() {
        CoachService service = new CoachService((s, p, schema) -> """
                {"headline":"h","summary":"s",
                 "exercises":[{"name":"bench press (barbell)","trend":"down","note":"Real."},
                              {"name":"Squat (Barbell)","trend":"up","note":"Made up!"}],
                 "tips":[]}
                """, mapper);

        CoachReview review = service.review(request);

        assertEquals(1, review.exercises().size()); // the squat note is gone
        assertEquals("Real.", review.exercises().get(0).note());
    }

    @Test
    void tidiesUnknownTrendsBlankHeadlinesAndTooManyTips() {
        CoachService service = new CoachService((s, p, schema) -> """
                {"headline":"  ","summary":"s",
                 "exercises":[{"name":"Bench Press (Barbell)","trend":"sideways","note":"n"}],
                 "tips":["a","b","c","d"," "]}
                """, mapper);

        CoachReview review = service.review(request);

        assertEquals("Workout reviewed", review.headline());
        assertEquals("same", review.exercises().get(0).trend());
        assertEquals(List.of("a", "b", "c"), review.tips());
    }

    @Test
    void missingListsBecomeEmptyNotNull() {
        CoachService service = new CoachService((s, p, schema) -> "{\"headline\":\"h\",\"summary\":\"s\"}", mapper);
        CoachReview review = service.review(request);
        assertEquals(List.of(), review.exercises());
        assertEquals(List.of(), review.tips());
    }

    @Test
    void anAnswerThatIsntJson_isAClearError() {
        CoachService service = new CoachService((s, p, schema) -> "Sorry, I can't help with that.", mapper);
        assertThrows(GeminiException.class, () -> service.review(request));
    }
}