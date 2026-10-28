package com.cinx.learning.catalog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // sá»‘ thread tá»‘i thiá»ƒu
        executor.setMaxPoolSize(20);       // sá»‘ thread tá»‘i Ä‘a
        executor.setQueueCapacity(100);    // queue chá»
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

