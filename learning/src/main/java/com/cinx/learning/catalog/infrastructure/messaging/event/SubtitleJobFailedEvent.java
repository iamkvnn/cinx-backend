package com.cinx.learning.catalog.infrastructure.messaging.event;

public record SubtitleJobFailedEvent(
        String jobId,
        String errorCode,
        String errorMessage
) {
}

