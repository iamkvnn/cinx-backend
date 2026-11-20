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
public class CourseQuestionCreatedEvent {
    private String eventId;
    private String questionId;
    private String courseId;
    private String lessonId;
    private String askedByUserId;
    private String questionTitle;
    private Instant occurredAt;
}
