package com.example.coach.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Counts coach requests per day (UTC) and refuses them past the limit, so a bug in the app
 * (like a request in a loop) can't burn through your free Gemini quota.
 *
 * Kept in memory, so it resets whenever the server restarts. On Render's free plan that happens
 * when it sleeps, so treat this as a seatbelt, not a vault: Gemini's own daily quota still applies.
 * The Clock is passed in so tests can pretend it's a different day.
 */
@Component
public class DailyLimiter {

    private final int limit;
    private final Clock clock;
    private LocalDate day;
    private int used;

    public DailyLimiter(CoachProperties properties, Clock clock) {
        this.limit = properties.dailyLimit();
        this.clock = clock;
        this.day = LocalDate.now(clock);
    }

    /** Uses up one request if any are left today. synchronized: two requests at once can't both take the last one. */
    public synchronized boolean tryAcquire() {
        rollOverIfNewDay();
        if (used >= limit) return false;
        used++;
        return true;
    }

    public synchronized int remainingToday() {
        rollOverIfNewDay();
        return Math.max(0, limit - used);
    }

    private void rollOverIfNewDay() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
    }
}