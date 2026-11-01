package com.cinx.learning.featured.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateFeaturedItemRequest(
        @NotNull
        @Schema(example = "false")
        Boolean active
) {
}
