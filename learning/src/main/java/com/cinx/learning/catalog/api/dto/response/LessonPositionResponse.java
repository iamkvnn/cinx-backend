package com.cinx.learning.catalog.api.dto.response;

public record LessonPositionResponse(
        String lessonId,
        String sectionId,
        Integer orderIndex
) {
}

