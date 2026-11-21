package com.example.coach.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyLimiterTest {

    /** A clock the test can move forward, to pretend a day has passed. */
    static class MovableClock extends Clock {
        Instant now = Instant.parse("2026-11-20T10:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MovableClock clock = new MovableClock();
    private final DailyLimiter limiter = new DailyLimiter(new CoachProperties("key", 2), clock);

    @Test
    void allowsUpToTheLimit_thenRefuses() {
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
        assertEquals(0, limiter.remainingToday());
    }

    @Test
    void resetsAtMidnightUtc() {
        limiter.tryAcquire();
        limiter.tryAcquire();
        assertFalse(limiter.tryAcquire());

        clock.now = Instant.parse("2026-11-21T00:00:01Z");

        assertEquals(2, limiter.remainingToday());
        assertTrue(limiter.tryAcquire());
    }
}