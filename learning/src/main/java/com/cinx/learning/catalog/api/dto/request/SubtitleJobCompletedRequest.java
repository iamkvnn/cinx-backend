package com.cinx.learning.catalog.api.dto.request;

public record SubtitleJobCompletedRequest(
        String jobId,
        String outputFileKey,
        String outputFileUrl,
        String fileName,
        String fileType,
        Long fileSize,
        String languageCode,
        String displayName,
        String wordConfidenceFileKey,
        String wordConfidenceFileUrl
) {
}

