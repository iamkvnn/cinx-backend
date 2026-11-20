package com.cinx.learning.assessment.application.evaluator;

import com.cinx.learning.assessment.domain.QuizSessionQuestion;

public interface IQuestionEvaluator {
    double evaluate(QuizSessionQuestion question);
}
