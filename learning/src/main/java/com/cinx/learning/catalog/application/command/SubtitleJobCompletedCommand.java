package com.cinx.learning.catalog.application.command;

/**
 * @param qualityReport the worker's quality summary as JSON, or null when the worker sent none
 */
public record SubtitleJobCompletedCommand(
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
        String qualityReport
) {
}
