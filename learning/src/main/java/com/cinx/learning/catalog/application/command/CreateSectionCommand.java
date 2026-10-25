package com.cinx.learning.catalog.application.command;

public record CreateSectionCommand(
        String title,
        String description,
        Long duration,
        Boolean aiAssisted
) {
}

