package com.cinx.learning.catalog.application.event;

public record SubtitleGenerateRequestedEvent(
        String jobId,
        String courseId,
        String lessonId,
        String videoFileKey,
        String targetLanguageCode,
        String displayName,
        String expectedOutputFileKey
) {
}

