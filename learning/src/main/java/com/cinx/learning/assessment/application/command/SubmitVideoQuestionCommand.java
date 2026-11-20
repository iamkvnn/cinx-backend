package com.cinx.learning.assessment.application.command;

public record SubmitVideoQuestionCommand(
        String videoAssessmentId,

        String userAnswer
) {
}
