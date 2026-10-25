package com.cinx.learning.catalog.application.command;

public record MoveLessonCommand(
        String targetSectionId,
        String previousLessonId,
        String nextLessonId
) {
}

