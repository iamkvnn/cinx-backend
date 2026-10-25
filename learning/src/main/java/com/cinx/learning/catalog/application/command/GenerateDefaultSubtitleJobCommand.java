package com.cinx.learning.catalog.application.command;

public record GenerateDefaultSubtitleJobCommand(
        String languageCode,
        String displayName
) {
}

