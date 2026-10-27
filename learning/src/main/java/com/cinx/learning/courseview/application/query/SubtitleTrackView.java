package com.cinx.learning.courseview.application.query;

public record SubtitleTrackView(
        String id,
        String languageCode,
        String displayName,
        String fileUrl,
        String fileKey,
        String fileName,
        String fileType,
        Long fileSize,
        String format,
        String source,
        String status,
        Boolean isDefault
) {
}
