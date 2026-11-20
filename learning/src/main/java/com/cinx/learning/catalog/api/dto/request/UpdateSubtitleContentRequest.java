package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateSubtitleContentRequest(
        @NotBlank
        @Schema(example = "WEBVTT\\n\\n00:00:01.000 --> 00:00:03.000\\nHello world\\n")
        String content,
        @Schema(example = "Vietnamese edited")
        String displayName
) {
}

