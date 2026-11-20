package com.cinx.learning.note.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record LessonNoteResponse(
        String id,
        String courseId,
        String lessonId,
        @Schema(description = "Set on the course-wide list, so notes can be grouped by lesson", example = "ProblemDetail là gì")
        String lessonTitle,
        String content,
        @Schema(description = "Seconds into the video; null for non-video lessons", example = "151")
        Integer positionSeconds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
