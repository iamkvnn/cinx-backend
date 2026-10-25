package com.cinx.learning.catalog.application.query;

import java.util.List;
import java.util.Map;

public record AdminCourseStatisticsOverviewView(
        Long createdCoursesInRange,
        Map<String, Long> createdCoursesByStatus,
        List<StatisticsByTimeView> createdCoursesByTime,
        Map<String, Long> currentCoursesByStatus,
        Long currentPublishedCount
) {
}

