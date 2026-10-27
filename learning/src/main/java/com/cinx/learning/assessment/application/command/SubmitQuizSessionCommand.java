package com.cinx.learning.assessment.application.command;

import java.util.List;

public record SubmitQuizSessionCommand(
    List<ChooseQuizAnswerCommand> answers
) {
}
