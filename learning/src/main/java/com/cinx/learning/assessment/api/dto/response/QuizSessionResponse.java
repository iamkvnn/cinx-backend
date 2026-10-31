package com.cinx.learning.assessment.api.dto.response;

import com.cinx.learning.shared.domain.QuizSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record QuizSessionResponse(
        @Schema(example = "sess_123")
        String id,
        @Schema(example = "user_123")
        String userId,
        @Schema(example = "les_123")
        String quizLessonId,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime startTime,
        @Schema(example = "2025-01-01T10:30:00")
        LocalDateTime endTime,
        @Schema(example = "COMPLETED")
        QuizSessionStatus status,
        @Schema(example = "true")
        Boolean isReviewAllowed,
        @Schema(example = "true")
        Boolean isShowAnswersOnReview,
        QuizSessionSubmissionResponse quizSessionSubmission
) {
}
