package com.cinx.learning.progress.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CourseProgressSummaryResponse(
        @Schema(example = "course_123")
        String courseId,
        @Schema(example = "4500")
        Long studentCount,
        @Schema(example = "2925")
        Long completedStudentCount,
        @Schema(example = "72.5")
        Double averageProgressPercent,
        @Schema(example = "65.0")
        Double completionRate
) {
}
