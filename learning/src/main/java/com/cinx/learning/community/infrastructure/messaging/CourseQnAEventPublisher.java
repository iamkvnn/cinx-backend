package com.cinx.learning.community.infrastructure.messaging;

import com.cinx.learning.community.application.CourseQnAEvents;

import com.cinx.learning.shared.infrastructure.messaging.OutboxEventPublisher;
import com.cinx.learning.community.application.event.CourseAnswerCreatedEvent;
import com.cinx.learning.community.application.event.CourseQuestionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseQnAEventPublisher implements CourseQnAEvents {
    private static final String EXCHANGE = "learning.events.exchange";

    private final OutboxEventPublisher outboxEventPublisher;

    public void publishQuestionCreatedEvent(CourseQuestionCreatedEvent event) {
        log.info("Publishing CourseQuestionCreatedEvent: {}", event.getEventId());
        outboxEventPublisher.enqueue(
                event.getEventId() != null ? event.getEventId() : UUID.randomUUID().toString(),
                "CourseQuestion",
                event.getQuestionId(),
                "CourseQuestionCreated",
                EXCHANGE,
                "question.created",
                event
        );
    }

    public void publishAnswerCreatedEvent(CourseAnswerCreatedEvent event) {
        log.info("Publishing CourseAnswerCreatedEvent: {}", event.getEventId());
        outboxEventPublisher.enqueue(
                event.getEventId() != null ? event.getEventId() : UUID.randomUUID().toString(),
                "CourseAnswer",
                event.getAnswerId(),
                "CourseAnswerCreated",
                EXCHANGE,
                "answer.created",
                event
        );
    }
}
