package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SectionResponse(
        @Schema(example = "sec_123")
        String id,
        @Schema(example = "Introduction to Spring Boot")
        String title,
        @Schema(example = "Basic concepts and setup")
        String description,
        @Schema(example = "3600")
        Long duration,
        @Schema(example = "1")
        Integer orderIndex
) {
}

