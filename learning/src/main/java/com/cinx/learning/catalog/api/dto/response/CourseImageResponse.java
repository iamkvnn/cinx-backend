package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CourseImageResponse(
    @Schema(example = "img_123")
    String id,
    @Schema(example = "https://example.com/images/course_123.jpg")
    String imageUrl
) {
}

