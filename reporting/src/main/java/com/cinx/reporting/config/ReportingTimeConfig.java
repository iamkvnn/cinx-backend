package com.cinx.reporting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReportingTimeConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
