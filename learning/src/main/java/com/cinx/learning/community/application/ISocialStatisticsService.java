package com.cinx.learning.community.application;

import com.cinx.learning.community.domain.StatisticsGroupBy;

import com.cinx.learning.community.application.query.CourseQnAStatisticsView;
import com.cinx.learning.community.application.query.ReportStatisticsOverviewView;
import com.cinx.learning.community.application.query.ReviewStatisticsView;

import java.time.LocalDate;

public interface ISocialStatisticsService {
    ReviewStatisticsView getReviewStatistics(String courseId);

    CourseQnAStatisticsView getCourseQnAStatistics(String courseId, StatisticsGroupBy groupBy, LocalDate startDate, LocalDate endDate);

    ReportStatisticsOverviewView getReportOverview(StatisticsGroupBy groupBy, LocalDate startDate, LocalDate endDate);
}
