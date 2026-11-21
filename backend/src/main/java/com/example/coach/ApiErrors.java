package com.example.coach;

import com.example.coach.gemini.GeminiException;
import com.example.coach.security.DailyLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Every error comes back as {"reason": ..., "error": ...}. The app switches on "reason"
 * (a fixed code) and can show "error" (a human message).
 */
@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<Map<String, String>> gemini(GeminiException e) {
        if (e.status() == 429) {
            return body(HttpStatus.TOO_MANY_REQUESTS, "gemini_quota", "The AI's free quota is used up for today");
        }
        return body(HttpStatus.BAD_GATEWAY, "gemini_error", e.getMessage());
    }

    @ExceptionHandler(DailyLimitExceededException.class)
    public ResponseEntity<Map<String, String>> dailyLimit(DailyLimitExceededException e) {
        return body(HttpStatus.TOO_MANY_REQUESTS, "daily_limit", e.getMessage());
    }

    /** Valid JSON, but breaking the rules (missing fields, too long, too many). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        String fields = String.join(", ", e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage()).toList());
        return body(HttpStatus.BAD_REQUEST, "invalid_request", "Invalid request: " + fields);
    }

    /** Not JSON at all, or the wrong shape. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException e) {
        return body(HttpStatus.BAD_REQUEST, "invalid_request", "The request body isn't valid JSON for this endpoint");
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String reason, String message) {
        return ResponseEntity.status(status).body(Map.of("reason", reason, "error", message));
    }
}