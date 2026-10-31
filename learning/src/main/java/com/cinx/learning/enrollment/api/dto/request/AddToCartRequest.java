package com.cinx.learning.enrollment.api.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;


public record AddToCartRequest(
    @Schema(example = "course_123")
    @NotBlank(message = "courseId must not be blank")
    String courseId
) {}
