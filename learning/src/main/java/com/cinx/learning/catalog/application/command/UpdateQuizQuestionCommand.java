package com.cinx.learning.catalog.application.command;

import com.cinx.learning.catalog.domain.ScoringMethod;

import java.util.List;

public record UpdateQuizQuestionCommand(
        String questionText,
        ScoringMethod scoringMethod,
        List<UpdateQuizOptionCommand> options,
        String referenceAnswer,
        String explanation
) {}

