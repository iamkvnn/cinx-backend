package com.cinx.notification.notification.infrastructure.messaging.event.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LessonChangedEvent {
    private String courseId;
    private String lessonId;
    private String changeType;
    /** Embedded by the course service to avoid a Feign call in the notification service. */
    private String courseTitle;
}