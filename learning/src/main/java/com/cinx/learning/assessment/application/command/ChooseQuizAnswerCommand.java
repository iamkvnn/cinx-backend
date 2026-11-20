package com.cinx.learning.assessment.application.command;

public record ChooseQuizAnswerCommand(
        String questionId,

        String userAnswer
) {
}
