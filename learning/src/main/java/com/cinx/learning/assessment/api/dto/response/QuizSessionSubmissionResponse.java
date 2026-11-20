package com.cinx.learning.assessment.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record QuizSessionSubmissionResponse(
        @Schema(example = "sub_123")
        String id,
        @Schema(example = "user_123")
        String userId,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime submissionTime,
        @Schema(example = "sess_123")
        String quizSessionId,
        @Schema(example = "8")
        Integer totalCorrectAnswers,
        @Schema(example = "80.0")
        Double score
) {
}
