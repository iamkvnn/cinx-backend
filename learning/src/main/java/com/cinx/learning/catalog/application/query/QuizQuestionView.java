package com.cinx.learning.catalog.application.query;

import com.cinx.learning.catalog.domain.QuizQuestionType;
import com.cinx.learning.catalog.domain.ScoringMethod;

import java.util.List;

public record QuizQuestionView(
        String id,
        String questionText,
        QuizQuestionType questionType,
        ScoringMethod scoringMethod,
        Boolean needSync,
        List<QuizOptionView> options,
        String referenceAnswer,
        String explanation,
        Boolean aiAssisted
) {
    /** What a quiz session copies for a learner: the explanation stays, the session shows it only on review. */
    public QuizQuestionView withoutReferenceAnswer() {
        return referenceAnswer == null ? this
                : new QuizQuestionView(id, questionText, questionType, scoringMethod, needSync, options, null, explanation,
                        aiAssisted);
    }

    /**
     * The view a learner gets before an attempt: no isCorrect, no reference answer, no explanation, and none of
     * the fields that encode the key by type (a short-text option is the accepted answer, a matchText is the pair,
     * an ordering's optionOrder is the sequence).
     */
    public QuizQuestionView withoutAnswerKeys() {
        List<QuizOptionView> hidden = options == null ? null
                : questionType == QuizQuestionType.SHORT_TEXT ? List.of()
                : options.stream()
                        .map(o -> new QuizOptionView(o.id(), o.optionText(), null,
                                questionType == QuizQuestionType.ORDERING ? null : o.optionOrder(),
                                questionType == QuizQuestionType.MATCHING ? null : o.matchText()))
                        .toList();
        return new QuizQuestionView(id, questionText, questionType, scoringMethod, needSync, hidden, null, null, aiAssisted);
    }
}
