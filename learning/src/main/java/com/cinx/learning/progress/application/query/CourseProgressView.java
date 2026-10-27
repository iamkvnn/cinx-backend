package com.cinx.learning.progress.application.query;

import java.time.LocalDateTime;

public record CourseProgressView(
    String id,
    String userId,
    String courseId,
    Boolean isCompleted,
    Boolean isPassed,
    Double avgScore,
    Integer totalItems,
    Integer completedItems,
    LocalDateTime completionTime
) {
}
