package com.cinx.learning.catalog.application.query;

import java.util.List;

public record InstructorCourseStatisticsOverviewView(
        Long createdCoursesInRange,
        List<StatisticsByTimeView> createdCoursesByTime,
        Long currentCourseCount,
        Long currentPublishedCourseCount,
        Double averageRating,
        Long currentEnrollmentSnapshot
) {
}

