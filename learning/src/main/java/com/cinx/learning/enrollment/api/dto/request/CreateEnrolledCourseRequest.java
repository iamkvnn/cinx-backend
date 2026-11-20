package com.cinx.learning.enrollment.api.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;


public record CreateEnrolledCourseRequest(
    @Schema(example = "course_123")
    @NotBlank(message = "courseId must not be blank")
    String courseId,

    @Schema(example = "user_123")
    @NotBlank(message = "userId must not be blank")
    String userId
) {}
