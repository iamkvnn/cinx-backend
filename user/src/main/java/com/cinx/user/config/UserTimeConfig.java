package com.cinx.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UserTimeConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
