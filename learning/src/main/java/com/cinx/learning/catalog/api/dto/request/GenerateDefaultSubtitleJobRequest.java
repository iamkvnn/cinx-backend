package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record GenerateDefaultSubtitleJobRequest(
        @Schema(example = "vi", description = "Optional. Omit to let faster-whisper auto-detect the source language.")
        String languageCode,
        @Schema(example = "Vietnamese")
        String displayName
) {
}

