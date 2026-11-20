package com.cinx.learning.engagement.application.query;

public record LearningActivityByTimeView(
        String timeLabel,
        Long activeSeconds
) {
}
