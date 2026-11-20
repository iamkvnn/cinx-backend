package com.cinx.learning.catalog.api.dto.response;

import java.util.List;

public record InstructorCourseStatisticsOverviewResponse(
        Long createdCoursesInRange,
        List<StatisticsByTimeResponse> createdCoursesByTime,
        Long currentCourseCount,
        Long currentPublishedCourseCount,
        Double averageRating,
        Long currentEnrollmentSnapshot
) {
}

