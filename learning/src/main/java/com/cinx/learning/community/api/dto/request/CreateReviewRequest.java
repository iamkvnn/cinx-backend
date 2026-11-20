package com.cinx.learning.community.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record CreateReviewRequest(
        @NotBlank(message = "courseId must not be blank")
        @Schema(example = "course_123")
        String courseId,

        @NotBlank(message = "content must not be blank")
        @Schema(example = "This course is amazing!")
        String content,

        @NotNull(message = "rating must not be null")
        @Schema(example = "5.0")
        Double rating
) {
}
