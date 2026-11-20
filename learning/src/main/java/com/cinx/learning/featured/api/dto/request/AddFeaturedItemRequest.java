package com.cinx.learning.featured.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Adds a course (featured courses) or a review (featured reviews) to the end of the featured list. */
public record AddFeaturedItemRequest(
        @NotBlank
        @Schema(description = "Course id for featured courses, review id for featured reviews")
        String targetId
) {
}
