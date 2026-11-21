package com.example.coach.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Runs before every /api/coach/** request: only callers that send the right X-App-Key get through.
 *
 * Honest limits: the key will be inside your app, and app files can be unpacked, so a determined
 * person could find it. It still stops bots that scan the internet for open servers, and the
 * daily limit caps the damage either way. A real product would add user accounts.
 */
@Component
public class AppKeyInterceptor implements HandlerInterceptor {

    static final String HEADER = "X-App-Key";

    private final CoachProperties properties;

    public AppKeyInterceptor(CoachProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String expected = properties.appKey();
        if (expected == null || expected.isBlank()) {
            // Safe default: with no key configured, the coach is closed rather than open to everyone.
            reject(response, 503, "server_misconfigured", "COACH_APP_KEY is not set on the server");
            return false;
        }
        String given = request.getHeader(HEADER);
        if (given == null || !sameKey(given, expected)) {
            reject(response, 401, "unauthorized", "Missing or wrong app key");
            return false;
        }
        return true;
    }

    /**
     * Compares in constant time: a normal equals() stops at the first wrong character, and
     * measuring that tiny time difference can leak the key one character at a time.
     */
    private static boolean sameKey(String given, String expected) {
        return MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }

    private static void reject(HttpServletResponse response, int status, String reason, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"reason\":\"" + reason + "\",\"error\":\"" + message + "\"}");
    }
}