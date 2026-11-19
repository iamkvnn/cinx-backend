package com.cinx.notification.notification.infrastructure.messaging.event.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseContentPublishedEvent {
    private String courseId;
    private String courseTitle;
    private String instructorId;
    private List<String> instructorIds;

    public CourseContentPublishedEvent(String courseId, String courseTitle) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
    }

    public CourseContentPublishedEvent(String courseId, String courseTitle, String instructorId) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.instructorId = instructorId;
    }
}
