package com.cinx.learning.engagement.application.query;

import java.util.List;

public record CourseEngagementOverviewView(
        Long activeLearnersInRange,
        Long totalLearningSeconds,
        Double averageProgressPercent,
        Double completionRate,
        List<LearningActivityByTimeView> activityByTime
) {
}
