package com.cinx.learning.assessment.application.query;

import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.ScoringMethod;

import java.util.List;

/** An official quiz question as a review shows it: the options a session would show, never the key. */
public record PracticeQuestionView(
        String questionId,
        String questionText,
        QuizQuestionType questionType,
        ScoringMethod scoringMethod,
        List<QuizSessionOptionView> options) {
}
