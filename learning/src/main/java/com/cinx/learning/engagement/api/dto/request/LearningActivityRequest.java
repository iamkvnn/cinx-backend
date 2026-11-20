package com.cinx.learning.engagement.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LearningActivityRequest(
        @Schema(example = "course_123")
        @NotBlank(message = "courseId must not be blank")
        String courseId,
        @Schema(example = "lesson_123")
        String itemId,
        @Schema(example = "60")
        @NotNull(message = "activeSeconds must not be null")
        Integer activeSeconds
) {
}
