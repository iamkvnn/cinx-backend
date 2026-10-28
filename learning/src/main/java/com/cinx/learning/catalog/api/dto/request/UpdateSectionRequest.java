package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateSectionRequest(
        @NotBlank
        @Schema(example = "Advanced Spring Boot")
        String title,
        @Schema(example = "Deep dive into Spring Boot configurations")
        String description,
        @Min(0)
        @Schema(example = "4200")
        Long duration
) {
}

