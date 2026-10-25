package com.cinx.learning.catalog.application.command;

public record CreateSubtitleTrackCommand(
        String languageCode,
        String displayName,
        String fileKey,
        String fileName,
        String fileType,
        Long fileSize,
        Boolean isDefault
) {
}

