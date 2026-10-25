package com.cinx.learning.catalog.application.command;

public record UpdateSectionCommand(
        String title,
        String description,
        Long duration
) {
}

