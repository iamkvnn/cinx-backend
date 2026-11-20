package com.cinx.learning.progress.application.command;

public record UpdateLearningItemCommand(
        Boolean isCompleted,
        Boolean isPassed,
        Double score
) {
}
