package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.LearnerSummaryView;

import java.util.List;

public interface ICourseLearnerService {
    /**
     * Name and avatar for each requested user who takes part in the course (entitled,
     * subscription access, or an active instructor). Callers must manage the course.
     */
    List<LearnerSummaryView> summaries(String currentUserId, String courseId, List<String> userIds);
}
