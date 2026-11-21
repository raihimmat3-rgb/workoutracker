package com.example.coach;

import com.example.coach.security.AppKeyInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppKeyInterceptor appKeyInterceptor;

    public WebConfig(AppKeyInterceptor appKeyInterceptor) {
        this.appKeyInterceptor = appKeyInterceptor;
    }

    /** The app key is needed for the coach, but not for /api/health (Render checks that). */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(appKeyInterceptor).addPathPatterns("/api/coach/**");
    }

    /** The real clock. Tests use a fake one instead. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}