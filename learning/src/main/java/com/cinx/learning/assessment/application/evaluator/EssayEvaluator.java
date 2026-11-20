package com.cinx.learning.assessment.application.evaluator;

import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import org.springframework.stereotype.Component;

/**
 * ESSAY evaluator — always returns 0.0 during auto-grading.
 * The score is intended to be manually set by an instructor afterwards.
 */
@Component
public class EssayEvaluator implements IQuestionEvaluator {

    @Override
    public double evaluate(QuizSessionQuestion question) {
        return 0.0;
    }
}
