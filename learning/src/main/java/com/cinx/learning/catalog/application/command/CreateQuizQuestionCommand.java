package com.cinx.learning.catalog.application.command;

import com.cinx.learning.catalog.domain.QuizQuestionType;
import com.cinx.learning.catalog.domain.ScoringMethod;

import java.util.List;

public record CreateQuizQuestionCommand(
        String questionText,
        QuizQuestionType questionType,
        ScoringMethod scoringMethod,
        List<CreateQuizOptionCommand> options,
        String referenceAnswer,
        String explanation,
        Boolean aiAssisted
) {
}

