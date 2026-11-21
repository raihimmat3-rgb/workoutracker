package com.example.coach.security;

public class DailyLimitExceededException extends RuntimeException {
    public DailyLimitExceededException() {
        super("Daily coach limit reached. It resets at midnight UTC.");
    }
}