package com.cinx.learning.catalog.application.command;

public record UpdateVideoOptionCommand(
        String id,
        String optionText,
        Boolean isCorrect
) {
}
