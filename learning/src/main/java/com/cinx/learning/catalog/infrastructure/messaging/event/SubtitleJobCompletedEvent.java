package com.cinx.learning.catalog.infrastructure.messaging.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @param quality the worker's quality summary; absent from workers that predate it
 */
public record SubtitleJobCompletedEvent(
        String jobId,
        String outputFileKey,
        String outputFileUrl,
        String fileName,
        String fileType,
        Long fileSize,
        String languageCode,
        String displayName,
        String wordConfidenceFileKey,
        String wordConfidenceFileUrl,
        JsonNode quality
) {
}
