package com.cinx.learning.progress.application.query;

public record LearningItemProgressView(
        String itemId,
        Boolean isCompleted,
        Boolean isPassed,
        Double score
) {
}
