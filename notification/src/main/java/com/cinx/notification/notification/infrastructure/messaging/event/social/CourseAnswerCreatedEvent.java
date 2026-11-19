package com.cinx.notification.notification.infrastructure.messaging.event.social;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAnswerCreatedEvent {
    private String eventId;
    private String answerId;
    private String questionId;
    private String courseId;
    private String questionAuthorId;
    private String parentAnswerAuthorId; // nullable
    private String answeredByUserId;
    private Instant occurredAt;
}