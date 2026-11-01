package com.cinx.learning.engagement.api.dto.response;

import java.util.List;

public record CourseEngagementOverviewResponse(
        Long activeLearnersInRange,
        Long totalLearningSeconds,
        Double averageProgressPercent,
        Double completionRate,
        List<LearningActivityByTimeResponse> activityByTime
) {
}
