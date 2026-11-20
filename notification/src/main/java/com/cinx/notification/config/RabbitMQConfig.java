package com.cinx.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange("cinx.domain.events").durable(true).build();
    }

    @Bean
    public TopicExchange courseExchange() {
        return ExchangeBuilder.topicExchange("learning.events.exchange").durable(true).build();
    }

    @Bean
    public TopicExchange qnaExchange() {
        return ExchangeBuilder.topicExchange("learning.events.exchange").durable(true).build();
    }

    @Bean
    public TopicExchange socialExchange() {
        return ExchangeBuilder.topicExchange("learning.events.exchange").durable(true).build();
    }

    @Bean
    public TopicExchange learningExchange() {
        return ExchangeBuilder.topicExchange("learning.events.exchange").durable(true).build();
    }

    @Bean
    public TopicExchange authExchange() {
        return ExchangeBuilder.topicExchange("auth.events.exchange").durable(true).build();
    }

    @Bean
    public TopicExchange userExchange() {
        return ExchangeBuilder.topicExchange("user.events.exchange").durable(true).build();
    }

    @Bean
    public Queue entitlementProjectionQueue() {
        return QueueBuilder.durable("notification.subscription-entitlement.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "entitlement.dead")
                .build();
    }

    @Bean
    public Binding entitlementProjectionBinding() {
        return BindingBuilder.bind(entitlementProjectionQueue())
                .to(paymentExchange())
                .with("commerce.subscription.entitlements.changed");
    }

    @Bean
    public Queue entitlementProjectionDeadLetterQueue() {
        return QueueBuilder.durable("notification.subscription-entitlement.dead.queue").build();
    }

    @Bean
    public Binding entitlementProjectionDeadLetterBinding() {
        return BindingBuilder.bind(entitlementProjectionDeadLetterQueue())
                .to(deadLetterExchange())
                .with("entitlement.dead");
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange("dlx.exchange").durable(true).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("notification.dlq").build();
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable("notification.payment.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "payment.dead")
                .build();
    }

    @Bean
    public Queue courseQueue() {
        return QueueBuilder.durable("notification.course.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "course.dead")
                .build();
    }

    @Bean
    public Queue courseApprovalQueue() {
        return QueueBuilder.durable("notification.course-approval.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "course.dead")
                .build();
    }

    @Bean
    public Queue socialQueue() {
        return QueueBuilder.durable("notification.social.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "social.dead")
                .build();
    }

    @Bean
    public Queue learningQueue() {
        return QueueBuilder.durable("notification.learning.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "learning.dead")
                .build();
    }

    @Bean
    public Queue authQueue() {
        return QueueBuilder.durable("notification.auth.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "auth.dead")
                .build();
    }

    @Bean
    public Queue userQueue() {
        return QueueBuilder.durable("notification.user.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "user.dead")
                .build();
    }

    @Bean
    public Queue campusQueue() {
        return QueueBuilder.durable("notification.campus.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "campus.dead")
                .build();
    }

    @Bean
    public Binding campusEventScheduledBinding() {
        return BindingBuilder.bind(campusQueue())
                .to(paymentExchange())
                .with("campus.event.scheduled");
    }

    @Bean
    public Binding campusEventReminderDueBinding() {
        return BindingBuilder.bind(campusQueue())
                .to(paymentExchange())
                .with("campus.event.reminder.due");
    }

    @Bean
    public Binding campusEventCancelledBinding() {
        return BindingBuilder.bind(campusQueue())
                .to(paymentExchange())
                .with("campus.event.cancelled");
    }

    @Bean
    public Binding campusThreadRepliedBinding() {
        return BindingBuilder.bind(campusQueue())
                .to(paymentExchange())
                .with("campus.thread.replied");
    }

    // its own queue, so the friend notices are not lost to notification.user.queue's user.# copy (UserNotificationListener skips them)
    @Bean
    public Queue friendQueue() {
        return QueueBuilder.durable("notification.friend.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "friend.dead")
                .build();
    }

    @Bean
    public Binding friendRequestedBinding() {
        return BindingBuilder.bind(friendQueue())
                .to(userExchange())
                .with("user.friend.requested");
    }

    @Bean
    public Binding friendAcceptedBinding() {
        return BindingBuilder.bind(friendQueue())
                .to(userExchange())
                .with("user.friend.accepted");
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue())
                .to(paymentExchange())
                .with("commerce.order.paid");
    }

    @Bean
    public Binding courseLessonChangedBinding() {
        return BindingBuilder.bind(courseQueue())
                .to(courseExchange())
                .with("course.content.published");
    }

    @Bean
    public Binding courseApprovalRequestedBinding() {
        return BindingBuilder.bind(courseApprovalQueue())
                .to(courseExchange())
                .with("course.approval.requested");
    }

    @Bean
    public Queue courseCollaboratorQueue() {
        return QueueBuilder.durable("notification.course-collaborator.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "course.dead")
                .build();
    }

    @Bean
    public Binding courseCollaboratorInvitedBinding() {
        return BindingBuilder.bind(courseCollaboratorQueue())
                .to(courseExchange())
                .with("course.collaborator.invited");
    }

    @Bean
    public Binding qnaQuestionCreatedBinding() {
        return BindingBuilder.bind(socialQueue())
                .to(qnaExchange())
                .with("question.created");
    }

    @Bean
    public Binding qnaAnswerCreatedBinding() {
        return BindingBuilder.bind(socialQueue())
                .to(qnaExchange())
                .with("answer.created");
    }

    @Bean
    public Binding socialReviewCreatedBinding() {
        return BindingBuilder.bind(socialQueue())
                .to(socialExchange())
                .with("social.review.created");
    }

    @Bean
    public Binding learningCourseCompletedBinding() {
        return BindingBuilder.bind(learningQueue())
                .to(learningExchange())
                .with("learning.course.completed");
    }

    @Bean
    public Binding learningReminderDueBinding() {
        return BindingBuilder.bind(learningQueue())
                .to(learningExchange())
                .with("learning.reminder.due");
    }

    @Bean
    public Binding learningStudyReminderDueBinding() {
        return BindingBuilder.bind(learningQueue())
                .to(learningExchange())
                .with("learning.study-reminder.due");
    }

    @Bean
    public Binding learningSubtitleJobFinishedBinding() {
        return BindingBuilder.bind(learningQueue())
                .to(learningExchange())
                .with("learning.subtitle.job.finished");
    }

    @Bean
    public Binding learningCertificateRequestedBinding() {
        return BindingBuilder.bind(learningQueue())
                .to(learningExchange())
                .with("learning.certificate.requested");
    }

    @Bean
    public Binding learningLabSessionReadyBinding() {
        return BindingBuilder.bind(learningQueue())
                .to(learningExchange())
                .with("learning.lab.session.ready");
    }

    @Bean
    public Binding learningLabSessionEndingBinding() {
        return BindingBuilder.bind(learningQueue())
                .to(learningExchange())
                .with("learning.lab.session.ending");
    }

    @Bean
    public Binding learningLabReviewDecidedBinding() {
        return BindingBuilder.bind(learningQueue())
                .to(learningExchange())
                .with("learning.lab.review.decided");
    }

    @Bean
    public Binding learningCertificateApprovedBinding() {
        return BindingBuilder.bind(learningQueue())
                .to(learningExchange())
                .with("learning.certificate.approved");
    }

    // auth.events.exchange — wildcard catches all OTP and account lifecycle events
    @Bean
    public Binding authEventsBinding() {
        return BindingBuilder.bind(authQueue())
                .to(authExchange())
                .with("auth.#");
    }

    // user.events.exchange — wildcard catches instructor and future user events
    @Bean
    public Binding userEventsBinding() {
        return BindingBuilder.bind(userQueue())
                .to(userExchange())
                .with("user.#");
    }

    @Bean
    public Binding dlqPaymentBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("payment.dead");
    }

    @Bean
    public Binding dlqCourseBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("course.dead");
    }

    @Bean
    public Binding dlqSocialBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("social.dead");
    }

    @Bean
    public Binding dlqLearningBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("learning.dead");
    }

    @Bean
    public Binding dlqOrderBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("order.dead");
    }

    @Bean
    public Binding dlqAuthBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("auth.dead");
    }

    @Bean
    public Binding dlqCampusBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("campus.dead");
    }

    @Bean
    public Binding dlqUserBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("user.dead");
    }

    @Bean
    public Binding dlqFriendBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("friend.dead");
    }
}
