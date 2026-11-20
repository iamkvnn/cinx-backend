package com.cinx.learning.featured.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Platform figures for the public home page, computed inside learning only:
 * learners = distinct users with a course enrollment, instructors = distinct owners of published courses.
 */
public record PublicStatsResponse(
        @Schema(example = "12400")
        long learnerCount,
        @Schema(example = "86")
        long instructorCount,
        @Schema(example = "128")
        long courseCount
) {
}
