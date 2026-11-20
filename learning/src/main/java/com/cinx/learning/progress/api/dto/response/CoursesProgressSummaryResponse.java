package com.cinx.learning.progress.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CoursesProgressSummaryResponse(
        @Schema(example = "13000")
        Long totalStudentProgressCount,
        @Schema(example = "8450")
        Long completedStudentProgressCount,
        @Schema(example = "65.0")
        Double completionRate,
        List<CourseProgressSummaryResponse> courses
) {
}
