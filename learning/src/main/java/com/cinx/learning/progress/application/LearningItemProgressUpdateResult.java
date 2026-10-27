package com.cinx.learning.progress.application;

public record LearningItemProgressUpdateResult(
        boolean completedTransition,
        boolean passedTransition,
        boolean courseCompletedTransition,
        boolean coursePassedTransition
) {
}
