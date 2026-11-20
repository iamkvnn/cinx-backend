package com.cinx.learning.assessment.application.query;

import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.ScoringMethod;

import java.util.List;

public record QuizSessionQuestionView(
        String id,
        String quizSessionId,
        String questionId,
        QuizQuestionType questionType,
        ScoringMethod scoringMethod,
        Integer questionOrder,
        String questionText,
        String userAnswer,
        String correctAnswer, // null when hidden (IN_PROGRESS or hideAnswers policy)
        Double score, // null when hidden
        List<QuizSessionOptionView> options,
        String explanation, // null when hidden
        Boolean aiAssisted) {
}
