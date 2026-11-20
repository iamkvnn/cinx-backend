package com.cinx.learning.engagement.application.command;

public record LearningActivityCommand(
        String courseId,
        String itemId,
        Integer activeSeconds
) {
}
