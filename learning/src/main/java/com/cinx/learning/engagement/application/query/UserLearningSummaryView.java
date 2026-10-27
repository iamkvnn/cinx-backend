package com.cinx.learning.engagement.application.query;

public record UserLearningSummaryView(
        Long completedCourseCount,
        Double averageProgressPercent,
        Long totalLearningSeconds
) {
}
