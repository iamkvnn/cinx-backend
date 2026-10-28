package com.cinx.learning.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean
    public TopicExchange domainEventsExchange() {
        return ExchangeBuilder.topicExchange("cinx.domain.events").durable(true).build();
    }

    @Bean
    public Queue commerceOrderPaidQueue() {
        return QueueBuilder.durable("learning.commerce-order-paid.queue")
                .deadLetterExchange("cinx.dlx").deadLetterRoutingKey("learning.commerce-order-paid.dlq").build();
    }

    @Bean
    public Binding commerceOrderPaidBinding() {
        return BindingBuilder.bind(commerceOrderPaidQueue()).to(domainEventsExchange()).with("commerce.order.paid");
    }

    @Bean
    public Queue subscriptionEntitlementQueue() {
        return QueueBuilder.durable("learning.subscription-entitlement.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "learning.subscription-entitlement.dead")
                .build();
    }

    @Bean
    public Queue subscriptionEntitlementDeadLetterQueue() {
        return QueueBuilder.durable("learning.subscription-entitlement.dead.queue").build();
    }

    @Bean
    public Binding subscriptionEntitlementBinding() {
        return BindingBuilder.bind(subscriptionEntitlementQueue()).to(domainEventsExchange())
                .with("commerce.subscription.entitlements.changed");
    }

    @Bean
    public Binding subscriptionEntitlementDeadLetterBinding() {
        return BindingBuilder.bind(subscriptionEntitlementDeadLetterQueue()).to(deadLetterExchange())
                .with("learning.subscription-entitlement.dead");
    }

    @Bean
    public Queue catalogEnrollmentGrantedQueue() {
        return QueueBuilder.durable("learning.catalog-enrollment-granted.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "learning.catalog-enrollment-granted.dead")
                .build();
    }

    @Bean
    public Queue catalogEnrollmentGrantedDeadLetterQueue() {
        return QueueBuilder.durable("learning.catalog-enrollment-granted.dead.queue").build();
    }

    @Bean
    public Binding catalogEnrollmentGrantedBinding() {
        return BindingBuilder.bind(catalogEnrollmentGrantedQueue())
                .to(learningEventsExchange())
                .with("learning.enrollment.granted");
    }

    @Bean
    public Binding catalogEnrollmentGrantedDeadLetterBinding() {
        return BindingBuilder.bind(catalogEnrollmentGrantedDeadLetterQueue())
                .to(deadLetterExchange())
                .with("learning.catalog-enrollment-granted.dead");
    }


    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange("dlx.exchange")
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange learningEventsExchange() {
        return ExchangeBuilder.topicExchange("learning.events.exchange")
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange aiSubtitleEventsExchange() {
        return ExchangeBuilder.topicExchange("ai.subtitle.events.exchange")
                .durable(true)
                .build();
    }

    @Bean
    public Queue subtitleAiQueue() {
        return QueueBuilder.durable("learning.subtitle.ai.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "learning.subtitle.ai.dead")
                .withArgument("x-max-length", 10000)
                .build();
    }

    @Bean
    public Queue subtitleAiDeadLetterQueue() {
        return QueueBuilder.durable("learning.subtitle.ai.dead.queue").build();
    }

    @Bean
    public Binding subtitleAiProgressBinding() {
        return BindingBuilder.bind(subtitleAiQueue())
                .to(aiSubtitleEventsExchange())
                .with("ai.subtitle.job.progress");
    }

    @Bean
    public Binding subtitleAiCompletedBinding() {
        return BindingBuilder.bind(subtitleAiQueue())
                .to(aiSubtitleEventsExchange())
                .with("ai.subtitle.job.completed");
    }

    @Bean
    public Binding subtitleAiFailedBinding() {
        return BindingBuilder.bind(subtitleAiQueue())
                .to(aiSubtitleEventsExchange())
                .with("ai.subtitle.job.failed");
    }

    @Bean
    public Binding subtitleAiDeadLetterBinding() {
        return BindingBuilder.bind(subtitleAiDeadLetterQueue())
                .to(deadLetterExchange())
                .with("learning.subtitle.ai.dead");
    }

    @Bean
    public Queue lessonChangeQueue() {
        return QueueBuilder.durable("learning.lesson-change.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "learning.lesson-change.dead")
                .withArgument("x-max-length", 10000)
                .build();
    }

    @Bean
    public Queue lessonChangeDeadLetterQueue() {
        return QueueBuilder.durable("learning.lesson-change.dead.queue").build();
    }

    @Bean
    public Binding lessonChangeBinding() {
        return BindingBuilder.bind(lessonChangeQueue())
                .to(learningEventsExchange())
                .with("course.content.published");
    }

    @Bean
    public Binding lessonChangeDeadLetterBinding() {
        return BindingBuilder.bind(lessonChangeDeadLetterQueue())
                .to(deadLetterExchange())
                .with("learning.lesson-change.dead");
    }

    @Bean
    public Queue quizSyncQueue() {
        return QueueBuilder.durable("learning.sync-and-regrade.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "learning.sync-and-regrade.dead")
                .withArgument("x-max-length", 10000)
                .build();
    }

    @Bean
    public Queue quizSyncDeadLetterQueue() {
        return QueueBuilder.durable("learning.sync-and-regrade.dead.queue").build();
    }

    @Bean
    public Binding quizSyncBinding() {
        return BindingBuilder.bind(quizSyncQueue())
                .to(learningEventsExchange())
                .with("course.quiz.sync-and-regrade");
    }

    @Bean
    public Binding quizSyncDeadLetterBinding() {
        return BindingBuilder.bind(quizSyncDeadLetterQueue())
                .to(deadLetterExchange())
                .with("learning.sync-and-regrade.dead");
    }

    @Bean
    public Queue quizScoringModeChangeQueue() {
        return QueueBuilder.durable("learning.quiz.scoring-mode-change.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "learning.quiz.scoring-mode-change.dead")
                .withArgument("x-max-length", 10000)
                .build();
    }

    @Bean
    public Queue quizScoringModeChangeDeadLetterQueue() {
        return QueueBuilder.durable("learning.quiz.scoring-mode-change.dead.queue").build();
    }

    @Bean
    public Binding quizScoringModeChangeBinding() {
        return BindingBuilder.bind(quizScoringModeChangeQueue())
                .to(learningEventsExchange())
                .with("course.quiz.scoring-mode-changed");
    }

    @Bean
    public Binding quizScoringModeChangeDeadLetterBinding() {
        return BindingBuilder.bind(quizScoringModeChangeDeadLetterQueue())
                .to(deadLetterExchange())
                .with("learning.quiz.scoring-mode-change.dead");
    }
}

