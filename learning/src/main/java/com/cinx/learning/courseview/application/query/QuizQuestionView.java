package com.cinx.learning.courseview.application.query;

import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.ScoringMethod;

import java.util.List;

public record QuizQuestionView(
        String id,
        String questionText,
        QuizQuestionType questionType,
        ScoringMethod scoringMethod,
        List<QuizOptionView> options,
        String explanation,
        Boolean aiAssisted
) {
}
