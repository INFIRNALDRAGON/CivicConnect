package com.civicconnect.backend.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** Injected everywhere that needs "now" so tests can pin the time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
