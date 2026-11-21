package com.example.coach.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param appKey     a shared secret the app sends in the X-App-Key header (COACH_APP_KEY).
 * @param dailyLimit most coach requests per day (UTC), to protect your free Gemini quota.
 */
@ConfigurationProperties(prefix = "coach")
public record CoachProperties(String appKey, int dailyLimit) {
}