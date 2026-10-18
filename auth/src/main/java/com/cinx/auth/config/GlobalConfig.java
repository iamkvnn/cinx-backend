package com.cinx.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Clock;

@Configuration
public class GlobalConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
