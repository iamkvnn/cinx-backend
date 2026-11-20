package com.cinx.reporting.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    /** Producers send JSON; without this the default converter hands listeners raw bytes and every Map listener fails. */
    @Bean MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean TopicExchange authExchange() { return new TopicExchange("auth.events.exchange"); }
    @Bean TopicExchange userExchange() { return new TopicExchange("user.events.exchange"); }
    @Bean TopicExchange domainEventsExchange() { return new TopicExchange("cinx.domain.events"); }
    @Bean TopicExchange learningExchange() { return new TopicExchange("learning.events.exchange"); }
    @Bean DirectExchange reportingDlx() { return new DirectExchange("dlx.exchange"); }

    @Bean Queue reportingAuthQueue() { return queue("reporting.auth.queue", "reporting.auth.dead"); }
    @Bean Queue reportingCommerceQueue() { return queue("reporting.commerce.queue", "reporting.commerce.dead"); }
    @Bean Queue reportingUserQueue() { return queue("reporting.user.queue", "reporting.user.dead"); }
    @Bean Queue reportingLearningQueue() { return queue("reporting.learning.queue", "reporting.learning.dead"); }
    @Bean Queue reportingEntitlementQueue() { return queue("reporting.subscription-entitlement.queue", "reporting.entitlement.dead"); }
    @Bean Queue reportingEntitlementDeadQueue() { return new Queue("reporting.entitlement.dead.queue", true); }
    @Bean Queue reportingAnalyticsConsentQueue() { return queue("reporting.analytics-consent.queue", "reporting.analytics-consent.dead"); }

    @Bean Binding reportingAuthBinding() {
        return BindingBuilder.bind(reportingAuthQueue()).to(authExchange()).with("auth.#");
    }
    @Bean Binding reportingCommerceBinding() {
        return BindingBuilder.bind(reportingCommerceQueue()).to(domainEventsExchange()).with("commerce.order.paid");
    }
    @Bean Binding reportingUserBinding() {
        return BindingBuilder.bind(reportingUserQueue()).to(userExchange()).with("user.instructor.#");
    }
    @Bean Binding reportingLearningBinding() {
        return BindingBuilder.bind(reportingLearningQueue()).to(learningExchange()).with("learning.enrollment.granted");
    }
    @Bean Binding reportingEntitlementBinding() {
        return BindingBuilder.bind(reportingEntitlementQueue()).to(domainEventsExchange())
                .with("commerce.subscription.entitlements.changed");
    }
    @Bean Binding reportingAnalyticsConsentBinding() {
        return BindingBuilder.bind(reportingAnalyticsConsentQueue()).to(userExchange()).with("user.learning-profile.updated");
    }
    @Bean Binding reportingEntitlementDeadBinding() {
        return BindingBuilder.bind(reportingEntitlementDeadQueue()).to(reportingDlx())
                .with("reporting.entitlement.dead");
    }

    private Queue queue(String name, String deadRoutingKey) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", deadRoutingKey)
                .build();
    }
}
