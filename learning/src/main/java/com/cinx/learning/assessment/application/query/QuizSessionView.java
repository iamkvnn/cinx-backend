package com.cinx.learning.assessment.application.query;

import com.cinx.learning.shared.domain.QuizSessionStatus;

import java.time.LocalDateTime;

public record QuizSessionView(
        String id,
        String userId,
        String quizLessonId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        QuizSessionStatus status,
        Boolean isReviewAllowed,
        Boolean isShowAnswersOnReview,
        QuizSessionSubmissionView quizSessionSubmission
) {
}
