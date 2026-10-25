package com.cinx.learning.catalog.application.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseContentPublishedEvent {
    private String courseId;
    private String courseTitle;
    private String instructorId;
    /** Snapshot of active instructors at publication time; optional for old events. */
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

