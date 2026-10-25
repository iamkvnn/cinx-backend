package com.cinx.learning.catalog.application.command;

public record UpdateQuizOptionCommand(
        String id,
        String optionText,
        Boolean isCorrect,
        Integer optionOrder,
        String matchText
) {}

