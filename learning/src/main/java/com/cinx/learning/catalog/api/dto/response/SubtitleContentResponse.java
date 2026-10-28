package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SubtitleContentResponse(
        @Schema(example = "sub_123")
        String subtitleId,
        @Schema(example = "WEBVTT\\n\\n00:00:01.000 --> 00:00:03.000\\nHello world\\n")
        String content
) {
}

