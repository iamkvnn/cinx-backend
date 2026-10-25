package com.cinx.learning.catalog.application.command;

import com.cinx.learning.catalog.domain.VideoQuizQuestionType;
import java.util.List;

public record CreateVideoQuestionCommand(
        String questionText,
        VideoQuizQuestionType questionType,
        Integer timestampSeconds,
        List<CreateVideoOptionCommand> options
) {
}

