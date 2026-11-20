package com.cinx.learning.catalog.api.dto.response;

public record LessonPreviewResponse (
        LessonResponse lesson,
        Object content
) {
}
