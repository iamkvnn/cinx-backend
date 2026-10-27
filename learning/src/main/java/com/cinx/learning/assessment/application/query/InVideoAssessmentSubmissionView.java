package com.cinx.learning.assessment.application.query;

import java.time.LocalDateTime;

public record InVideoAssessmentSubmissionView(
        String videoLessonId,
        String videoAssessmentId,
        String userAnswer,
        LocalDateTime submissionTime
) {
}
