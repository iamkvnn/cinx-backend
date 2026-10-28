package com.cinx.learning.catalog.infrastructure.messaging.event;

public record SubtitleJobProgressEvent(
        String jobId,
        Integer progressPercent
) {
}

