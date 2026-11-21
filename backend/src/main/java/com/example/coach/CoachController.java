package com.example.coach;

import com.example.coach.review.CoachReview;
import com.example.coach.review.CoachService;
import com.example.coach.review.ReviewRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CoachController {

    private final CoachService coach;

    public CoachController(CoachService coach) {
        this.coach = coach;
    }

    /** "Is the server up?" Free: never calls Gemini. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    /** Reviews one finished workout. Uses one Gemini request. */
    @PostMapping("/coach/review")
    public CoachReview review(@Valid @RequestBody ReviewRequest request) {
        return coach.review(request);
    }
}