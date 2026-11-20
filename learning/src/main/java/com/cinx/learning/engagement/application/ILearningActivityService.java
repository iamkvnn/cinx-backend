package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.domain.LearningActivityGroupBy;

import com.cinx.learning.engagement.application.command.LearningActivityCommand;
import com.cinx.learning.progress.application.query.CourseProgressView;
import com.cinx.learning.progress.application.query.CoursesProgressSummaryView;
import com.cinx.learning.engagement.application.query.LearningActivityByTimeView;
import com.cinx.learning.engagement.application.query.LearningActivitySummaryView;
import com.cinx.learning.engagement.application.query.UserLearningSummaryView;

import java.time.LocalDate;
import java.util.List;

public interface ILearningActivityService {
    void recordActivity(String userId, LearningActivityCommand request);
    void recordActivity(String userId, String courseId, Integer activeSeconds);
    UserLearningSummaryView getUserLearningSummary(String userId);
    LearningActivitySummaryView getMyActivitySummary(String userId);
    List<CourseProgressView> getUserCourseProgress(String userId, List<String> courseIds);
    List<LearningActivityByTimeView> getUserActivitySeries(String userId, LearningActivityGroupBy groupBy, LocalDate startDate, LocalDate endDate);
    CoursesProgressSummaryView getCoursesProgressSummary(List<String> courseIds);
}
