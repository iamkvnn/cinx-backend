package com.cinx.learning.community.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record UpdateReviewRequest(
        @NotBlank(message = "content must not be blank")
        @Schema(example = "Updated review content")
        String content,

        @NotNull(message = "rating must not be null")
        @Schema(example = "5.0")
        Double rating
) {
}
