package com.cinx.learning.featured.api.dto.response;

import com.cinx.learning.catalog.api.dto.response.CourseResponse;
import io.swagger.v3.oas.annotations.media.Schema;

public record FeaturedCourseResponse(
        @Schema(example = "featured_123")
        String id,
        @Schema(example = "0")
        Integer sortOrder,
        @Schema(example = "true")
        Boolean active,
        CourseResponse course
) {
}
