package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Category as listed by the public catalog, with its number of published courses.
 * Kept apart from {@link CategoryResponse}, which is embedded in course responses and events.
 */
public record CategorySummaryResponse(
    @Schema(example = "cat_123")
    String id,
    @Schema(example = "Software Development")
    String name,
    @Schema(example = "42")
    long courseCount
) {
}
