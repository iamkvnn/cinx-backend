package com.cinx.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthRabbitMQConfig {

    @Bean
    public TopicExchange authEventsExchange() {
        return new TopicExchange("auth.events.exchange");
    }

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange("user.events.exchange");
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dlx.exchange");
    }

    @Bean
    public Queue identityUserLifecycleQueue() {
        return QueueBuilder.durable("auth.user-lifecycle.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "auth.user-lifecycle.dead")
                .build();
    }

    @Bean
    public Queue identityUserLifecycleDeadQueue() {
        return QueueBuilder.durable("auth.user-lifecycle.dead.queue").build();
    }

    @Bean
    public Binding profileOnboardedBinding() {
        return BindingBuilder.bind(identityUserLifecycleQueue())
                .to(userEventsExchange())
                .with("user.profile.onboarded");
    }

    @Bean
    public Binding instructorVerifiedBinding() {
        return BindingBuilder.bind(identityUserLifecycleQueue())
                .to(userEventsExchange())
                .with("user.instructor.verified");
    }

    @Bean
    public Binding instructorTerminatedBinding() {
        return BindingBuilder.bind(identityUserLifecycleQueue())
                .to(userEventsExchange())
                .with("user.instructor.terminated");
    }

    @Bean
    public Binding identityUserLifecycleDeadBinding() {
        return BindingBuilder.bind(identityUserLifecycleDeadQueue())
                .to(deadLetterExchange())
                .with("auth.user-lifecycle.dead");
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}
