package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateVideoQuestionRequest(
        @NotBlank
        @Schema(example = "Updated question text?")
        String questionText,
        @Min(0)
        @Schema(example = "150")
        Integer timestampSeconds,
        @Valid
        List<UpdateVideoOptionRequest> options
) {
}
