package com.cinx.learning.assessment.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record InVideoAssessmentSubmissionResponse(
        @Schema(example = "vlesson_123")
        String videoLessonId,
        @Schema(example = "vass_123")
        String videoAssessmentId,
        @Schema(example = "opt_123")
        String userAnswer,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime submissionTime
) {
}
