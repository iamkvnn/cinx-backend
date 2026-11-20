package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record UpdateSubtitleTrackRequest(
        @Schema(example = "Vietnamese")
        String displayName,
        @Schema(example = "courses/subtitles/uploads/uuid-intro.vi.vtt")
        String fileKey,
        @Schema(example = "intro.vi.vtt")
        String fileName,
        @Schema(example = "text/vtt")
        String fileType,
        @Min(1)
        @Schema(example = "4096")
        Long fileSize,
        @Schema(example = "true")
        Boolean isDefault
) {
}

