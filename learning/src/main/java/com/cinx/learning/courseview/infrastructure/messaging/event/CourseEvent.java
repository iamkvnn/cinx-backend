package com.cinx.learning.courseview.infrastructure.messaging.event;

import com.cinx.learning.courseview.application.query.CourseView;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseEvent {
    private CourseView course;
    private LocalDateTime timestamp;
}
