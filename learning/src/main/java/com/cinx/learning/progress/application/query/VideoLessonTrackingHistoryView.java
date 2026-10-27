package com.cinx.learning.progress.application.query;

public record VideoLessonTrackingHistoryView(
        String userId,
        String videoLessonId,
        Integer currentPosition,
        String lastTrackingTime
) {
}
