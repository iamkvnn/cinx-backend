package com.cinx.learning.assessment.application.command;

import java.util.List;

public record GradeEssayCommand(
        List<EssayQuestionScore> scores
) {
    public record EssayQuestionScore(
             String questionId,
            Double score
    ) {}
}
