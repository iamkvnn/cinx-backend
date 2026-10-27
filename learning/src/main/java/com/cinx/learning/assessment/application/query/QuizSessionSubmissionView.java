package com.cinx.learning.assessment.application.query;

import java.time.LocalDateTime;

public record QuizSessionSubmissionView(
        String id,
        String userId,
        LocalDateTime submissionTime,
        String quizSessionId,
        Integer totalCorrectAnswers,
        Double score
) {
}
