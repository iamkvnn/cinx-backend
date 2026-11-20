package com.cinx.learning.assessment.application.query;

public record QuizQuestionAnalyticsView(
    String questionId,
    Integer totalAttempts,
    Integer correctAttempts,
    Double accuracy
) {}