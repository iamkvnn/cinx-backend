package com.cinx.learning.note.application.query;

import java.time.LocalDateTime;

public record LessonNoteView(
        String id,
        String courseId,
        String lessonId,
        String lessonTitle,
        String content,
        Integer positionSeconds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
