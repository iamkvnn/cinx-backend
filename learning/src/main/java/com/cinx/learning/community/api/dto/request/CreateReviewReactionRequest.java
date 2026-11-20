package com.cinx.learning.community.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateReviewReactionRequest(
    @NotNull(message = "liked must not be null")
    @Schema(example = "true")
    Boolean liked
) {
}
