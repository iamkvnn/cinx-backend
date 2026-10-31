package com.cinx.learning.assessment.application.evaluator;

import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.ScoringMethod;
import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import org.springframework.stereotype.Component;

/**
 * Factory that resolves the correct {@link IQuestionEvaluator} for a given question.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>ESSAY      — always {@link EssayEvaluator} (manual grading only).</li>
 *   <li>SHORT_TEXT — always {@link ShortTextEvaluator} (any-match, case-insensitive).</li>
 *   <li>Others     — delegate to the evaluator matching the question's {@link ScoringMethod}.</li>
 * </ol>
 */
@Component
public class QuestionEvaluatorFactory {

    private final IQuestionEvaluator ALL_OR_NOTHING;
    private final IQuestionEvaluator PARTIAL_CREDIT;
    private final IQuestionEvaluator NEGATIVE_MARK;
    private final IQuestionEvaluator ESSAY;
    private final IQuestionEvaluator SHORT_TEXT;

    public QuestionEvaluatorFactory(AllOrNothingEvaluator allOrNothing,
                                    PartialCreditEvaluator partialCredit,
                                    NegativeMarkEvaluator negativeMark,
                                    EssayEvaluator essay,
                                    ShortTextEvaluator shortText) {
        this.ALL_OR_NOTHING = allOrNothing;
        this.PARTIAL_CREDIT = partialCredit;
        this.NEGATIVE_MARK  = negativeMark;
        this.ESSAY          = essay;
        this.SHORT_TEXT     = shortText;
    }

    public IQuestionEvaluator resolve(QuizSessionQuestion question) {
        if (question.getQuestionType() == QuizQuestionType.ESSAY) {
            return ESSAY;
        }
        if (question.getQuestionType() == QuizQuestionType.SHORT_TEXT) {
            return SHORT_TEXT;
        }
        if (question.getScoringMethod() == null) {
            return ALL_OR_NOTHING;
        }
        return switch (question.getScoringMethod()) {
            case PARTIAL_CREDIT -> PARTIAL_CREDIT;
            case NEGATIVE_MARK  -> NEGATIVE_MARK;
            default             -> ALL_OR_NOTHING;
        };
    }
}
