package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.application.query.CourseEngagementOverviewView;
import com.cinx.learning.engagement.domain.LearningActivityGroupBy;

import java.time.LocalDate;

public interface ILearningEngagementStatisticsService {
    CourseEngagementOverviewView getInstructorCourseEngagement(String currentUserId, String courseId, LearningActivityGroupBy groupBy, LocalDate startDate, LocalDate endDate);

    CourseEngagementOverviewView getAdminCourseEngagement(String courseId, LearningActivityGroupBy groupBy, LocalDate startDate, LocalDate endDate);
}
