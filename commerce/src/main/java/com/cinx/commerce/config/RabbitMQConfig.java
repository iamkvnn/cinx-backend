package com.cinx.commerce.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.*;

@Configuration
public class RabbitMQConfig {
    @Bean
    TopicExchange domainEventsExchange() {
        return new TopicExchange("cinx.domain.events", true, false);
    }

    @Bean
    TopicExchange authEventsExchange() {
        return new TopicExchange("auth.events.exchange", true, false);
    }

    @Bean
    TopicExchange userEventsExchange() {
        return new TopicExchange("user.events.exchange", true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange("cinx.dlx", true, false);
    }

    @Bean
    Queue courseSellabilityQueue() {
        return QueueBuilder.durable("commerce.course-sellability")
                .deadLetterExchange("cinx.dlx").deadLetterRoutingKey("commerce.course-sellability.dlq").build();
    }

    @Bean
    Binding courseSellabilityBinding(Queue courseSellabilityQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(courseSellabilityQueue).to(domainEventsExchange).with("learning.course.sellability.changed");
    }

    @Bean
    Queue courseSellabilityDeadLetterQueue() {
        return QueueBuilder.durable("commerce.course-sellability.dlq").build();
    }

    @Bean
    Binding courseSellabilityDeadLetterBinding(Queue courseSellabilityDeadLetterQueue,
                                                DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(courseSellabilityDeadLetterQueue).to(deadLetterExchange)
                .with("commerce.course-sellability.dlq");
    }

    @Bean
    Queue learningPathCheckoutQueue() {
        return QueueBuilder.durable("commerce.learning-path-checkout")
                .deadLetterExchange("cinx.dlx").deadLetterRoutingKey("commerce.learning-path-checkout.dlq").build();
    }

    @Bean
    Queue learningPathCheckoutDeadLetterQueue() {
        return QueueBuilder.durable("commerce.learning-path-checkout.dlq").build();
    }

    @Bean
    Binding learningPathCheckoutDeadLetterBinding(Queue learningPathCheckoutDeadLetterQueue,
                                                  DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(learningPathCheckoutDeadLetterQueue).to(deadLetterExchange)
                .with("commerce.learning-path-checkout.dlq");
    }

    @Bean
    Queue subscriptionEntitlementReconciliationQueue() {
        return QueueBuilder.durable("commerce.subscription-entitlement-reconciliation")
                .deadLetterExchange("cinx.dlx")
                .deadLetterRoutingKey("commerce.subscription-entitlement-reconciliation.dlq")
                .build();
    }

    @Bean
    Queue authAuthorizationQueue() {
        return QueueBuilder.durable("commerce.auth-authorization.queue")
                .deadLetterExchange("cinx.dlx")
                .deadLetterRoutingKey("commerce.auth-authorization.dlq")
                .build();
    }

    @Bean
    Binding authAuthorizationBinding(Queue authAuthorizationQueue, TopicExchange authEventsExchange) {
        return BindingBuilder.bind(authAuthorizationQueue).to(authEventsExchange)
                .with("auth.authorization.changed");
    }

    @Bean
    Queue authAuthorizationDeadLetterQueue() {
        return QueueBuilder.durable("commerce.auth-authorization.dlq").build();
    }

    @Bean
    Binding authAuthorizationDeadLetterBinding(Queue authAuthorizationDeadLetterQueue,
                                               DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(authAuthorizationDeadLetterQueue).to(deadLetterExchange)
                .with("commerce.auth-authorization.dlq");
    }

    @Bean
    Queue userOnboardingQueue() {
        return QueueBuilder.durable("commerce.user-onboarding.queue")
                .deadLetterExchange("cinx.dlx")
                .deadLetterRoutingKey("commerce.user-onboarding.dlq")
                .build();
    }

    @Bean
    Binding userOnboardingBinding(Queue userOnboardingQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userOnboardingQueue).to(userEventsExchange)
                .with("user.profile.onboarded");
    }

    @Bean
    Queue userOnboardingDeadLetterQueue() {
        return QueueBuilder.durable("commerce.user-onboarding.dlq").build();
    }

    @Bean
    Binding userOnboardingDeadLetterBinding(Queue userOnboardingDeadLetterQueue,
                                            DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(userOnboardingDeadLetterQueue).to(deadLetterExchange)
                .with("commerce.user-onboarding.dlq");
    }

    @Bean
    Binding learningPathCheckoutBinding(Queue learningPathCheckoutQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(learningPathCheckoutQueue).to(domainEventsExchange)
                .with("learning.path.checkout.requested");
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
