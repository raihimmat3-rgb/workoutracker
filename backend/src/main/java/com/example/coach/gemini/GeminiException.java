package com.example.coach.gemini;

/** Anything that went wrong talking to Gemini: no key, an error response, or an empty answer. */
public class GeminiException extends RuntimeException {

    private final int status;

    public GeminiException(String message, int status) {
        super(message);
        this.status = status;
    }

    public GeminiException(String message, int status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /** Gemini's HTTP status (e.g. 429 = over the free quota), or 0 if it never answered. */
    public int status() {
        return status;
    }
}
