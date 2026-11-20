package com.cinx.learning.catalog.api.dto.response;

import java.util.List;
import java.util.Map;

public record AdminCourseStatisticsOverviewResponse(
        Long createdCoursesInRange,
        Map<String, Long> createdCoursesByStatus,
        List<StatisticsByTimeResponse> createdCoursesByTime,
        Map<String, Long> currentCoursesByStatus,
        Long currentPublishedCount
) {
}

