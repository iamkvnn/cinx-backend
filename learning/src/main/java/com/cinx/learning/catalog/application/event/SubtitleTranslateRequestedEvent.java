package com.cinx.learning.catalog.application.event;

public record SubtitleTranslateRequestedEvent(
        String jobId,
        String courseId,
        String lessonId,
        String videoFileKey,
        String sourceSubtitleId,
        String sourceLanguageCode,
        String sourceFileKey,
        String sourceFileUrl,
        String targetLanguageCode,
        String displayName,
        String expectedOutputFileKey
) {
}

