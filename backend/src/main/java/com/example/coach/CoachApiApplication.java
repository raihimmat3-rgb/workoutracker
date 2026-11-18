package com.example.coach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The coach server. The Android app sends it workout data; it asks Gemini to explain it and
 * sends the answer back. The Gemini key lives only here (as an environment variable), never in the app.
 */
@SpringBootApplication
@ConfigurationPropertiesScan // finds GeminiProperties
public class CoachApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoachApiApplication.class, args);
    }
}
