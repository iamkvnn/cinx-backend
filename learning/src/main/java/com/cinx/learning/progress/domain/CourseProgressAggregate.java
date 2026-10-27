package com.cinx.learning.progress.domain;

public record CourseProgressAggregate(
        int totalItems,
        int completedItems,
        double avgScore,
        boolean completed,
        boolean passed
) {
}
