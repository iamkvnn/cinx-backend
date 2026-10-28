package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SubtitleWordConfidenceResponse(
        @Schema(example = "sub_123")
        String subtitleId,
        List<SubtitleWordConfidenceItem> words
) {
    public record SubtitleWordConfidenceItem(
            @Schema(example = "Hello")
            String word,
            @Schema(example = "1.25")
            Double start,
            @Schema(example = "1.55")
            Double end,
            @Schema(example = "0.91")
            Double probability
    ) {
    }
}

