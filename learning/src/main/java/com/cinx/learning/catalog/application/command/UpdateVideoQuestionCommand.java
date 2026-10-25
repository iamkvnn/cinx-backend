package com.cinx.learning.catalog.application.command;

import java.util.List;

public record UpdateVideoQuestionCommand(
        String questionText,
        Integer timestampSeconds,
        List<UpdateVideoOptionCommand> options
) {
}
