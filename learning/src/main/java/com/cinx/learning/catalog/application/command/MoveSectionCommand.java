package com.cinx.learning.catalog.application.command;

public record MoveSectionCommand(
        String previousSectionId,
        String nextSectionId
) {
}

