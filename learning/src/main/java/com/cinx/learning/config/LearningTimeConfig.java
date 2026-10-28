package com.cinx.learning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class LearningTimeConfig {
    @Bean
    Clock learningClock() {
        return Clock.systemUTC();
    }
}
