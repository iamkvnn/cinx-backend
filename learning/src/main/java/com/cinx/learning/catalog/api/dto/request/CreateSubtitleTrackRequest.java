package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSubtitleTrackRequest(
        @NotBlank
        @Schema(example = "vi")
        String languageCode,
        @NotBlank
        @Schema(example = "Vietnamese")
        String displayName,
        @NotBlank
        @Schema(example = "courses/subtitles/uploads/uuid-intro.vi.srt")
        String fileKey,
        @NotBlank
        @Schema(example = "intro.vi.srt")
        String fileName,
        @NotBlank
        @Schema(example = "application/x-subrip")
        String fileType,
        @NotNull
        @Min(1)
        @Schema(example = "4096")
        Long fileSize,
        @Schema(example = "true")
        Boolean isDefault
) {
}

