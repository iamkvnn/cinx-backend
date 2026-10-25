package com.cinx.learning.catalog.application.command;

public record UpdateSubtitleTrackCommand(
        String displayName,
        String fileKey,
        String fileName,
        String fileType,
        Long fileSize,
        Boolean isDefault
) {
}

