package com.cinx.learning.catalog.application.command;

public record CreateQuizOptionCommand(
        String optionText,
        Boolean isCorrect,
        Integer optionOrder,
        String matchText
) {
}

