package com.cinx.learning.community.application.event;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class CourseQuestionCreatedEvent {
    private String eventId;
    private String questionId;
    private String courseId;
    private String lessonId;
    private String askedByUserId;
    private String questionTitle;
    private Instant occurredAt;
}
