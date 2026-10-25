package com.cinx.learning.catalog.application.command;

public record UpdateSubtitleContentCommand(
        String content,
        String displayName
) {
}

