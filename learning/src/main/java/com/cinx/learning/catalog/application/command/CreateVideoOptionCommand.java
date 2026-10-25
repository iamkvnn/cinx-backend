package com.cinx.learning.catalog.application.command;

public record CreateVideoOptionCommand(
        String optionText,
        Boolean isCorrect
) {
}

