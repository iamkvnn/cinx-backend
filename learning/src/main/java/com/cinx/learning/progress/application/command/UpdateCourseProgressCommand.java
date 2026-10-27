package com.cinx.learning.progress.application.command;

import java.time.LocalDateTime;

public record UpdateCourseProgressCommand(
    Boolean isCompleted,

    Boolean isPassed,

    Double avgScore,

    Integer totalItems,

    Integer completedItems,

    LocalDateTime completionTime
) {
}
