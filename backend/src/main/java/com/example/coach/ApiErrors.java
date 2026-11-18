package com.example.coach;

import com.example.coach.gemini.GeminiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Turns failures into clear JSON errors instead of Spring's default error page. */
@RestControllerAdvice
public class ApiErrors {

    /** Gemini failed. 429 passes through so the app can say "daily limit reached". */
    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<Map<String, String>> gemini(GeminiException e) {
        HttpStatus status = e.status() == 429 ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }

    /** The request itself was invalid (e.g. an empty or too-long question). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request: " + e.getBindingResult().getFieldErrors()
                .stream().map(f -> f.getField() + " " + f.getDefaultMessage()).toList()));
    }
}
