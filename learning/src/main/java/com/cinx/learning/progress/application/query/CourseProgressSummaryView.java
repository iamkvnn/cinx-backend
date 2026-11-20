package com.cinx.learning.progress.application.query;

public record CourseProgressSummaryView(
        String courseId,
        Long studentCount,
        Long completedStudentCount,
        Double averageProgressPercent,
        Double completionRate
) {
}
