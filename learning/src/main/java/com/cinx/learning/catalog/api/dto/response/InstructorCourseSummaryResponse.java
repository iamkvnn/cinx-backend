package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record InstructorCourseSummaryResponse(
        @Schema(example = "8")
        Long courseCount,
        @Schema(example = "6")
        Long publishedCourseCount,
        @Schema(example = "4.8")
        Double averageRating
) {
}

