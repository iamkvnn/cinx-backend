package com.cinx.learning.catalog.application.query;

public record LessonPositionView(
        String lessonId,
        String sectionId,
        Integer orderIndex
) {
}

