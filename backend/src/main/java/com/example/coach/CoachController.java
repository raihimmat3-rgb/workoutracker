package com.example.coach;

import com.example.coach.gemini.GeminiClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CoachController {

    private final GeminiClient gemini;

    public CoachController(GeminiClient gemini) {
        this.gemini = gemini;
    }

    /** "Is the server up?" Free: never calls Gemini. Render will also use this later. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    /**
     * TEMPORARY (Day 29): proves your key and model work. Replaced by the real coach endpoints.
     * Each call uses one of your free daily Gemini requests.
     */
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return new AskResponse(gemini.generate(request.question()));
    }

    public record AskRequest(@NotBlank @Size(max = 500) String question) {}

    public record AskResponse(String answer) {}
}
