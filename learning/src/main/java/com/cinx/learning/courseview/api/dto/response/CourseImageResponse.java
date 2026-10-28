package com.cinx.learning.courseview.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CourseImageResponse(
    @Schema(example = "img_123")
    String id,
    @Schema(example = "https://example.com/image.jpg")
    String imageUrl
) {
}
