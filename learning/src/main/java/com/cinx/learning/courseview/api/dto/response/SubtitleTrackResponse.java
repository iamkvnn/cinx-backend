package com.cinx.learning.courseview.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SubtitleTrackResponse(
        @Schema(example = "sub_123")
        String id,
        @Schema(example = "vi")
        String languageCode,
        @Schema(example = "Vietnamese")
        String displayName,
        @Schema(example = "https://cdn.example.com/courses/subtitles/lesson-1/vi/sub_123.vtt")
        String fileUrl,
        @Schema(example = "courses/subtitles/lesson-1/vi/sub_123.vtt")
        String fileKey,
        @Schema(example = "intro.vi.srt")
        String fileName,
        @Schema(example = "text/vtt")
        String fileType,
        @Schema(example = "4096")
        Long fileSize,
        @Schema(example = "VTT")
        String format,
        @Schema(example = "MANUAL")
        String source,
        @Schema(example = "READY")
        String status,
        @Schema(example = "true")
        Boolean isDefault
) {
}
