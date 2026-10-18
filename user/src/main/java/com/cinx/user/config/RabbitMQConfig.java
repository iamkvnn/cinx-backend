package com.cinx.user.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange userEventsExchange() {
        return ExchangeBuilder.topicExchange("user.events.exchange").durable(true).build();
    }
}
