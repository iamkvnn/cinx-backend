package com.cinx.learning.catalog.application.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseApprovalRequestedEvent {
    private String courseId;
    private String courseTitle;
    private String instructorId;
}

