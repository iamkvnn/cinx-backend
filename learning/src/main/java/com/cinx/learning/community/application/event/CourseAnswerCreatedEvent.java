package com.cinx.learning.community.application.event;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
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