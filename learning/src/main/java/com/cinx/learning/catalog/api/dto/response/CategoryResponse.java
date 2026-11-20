package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CategoryResponse(
    @Schema(example = "cat_123")
    String id,
    @Schema(example = "Software Development")
    String name
) {
}

