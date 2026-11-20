package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TranslateSubtitleJobRequest(
        @Schema(example = "sub_123")
        String sourceSubtitleId,
        @NotEmpty
        @Schema(example = "[\"en\", \"fr\"]")
        List<@NotBlank String> targetLanguageCodes
) {
}

