package com.cinx.learning.catalog.api.dto.response;


import com.cinx.learning.shared.domain.LessonType;

import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record LessonResponse (
        @Schema(example = "les_123")
        String id,
        @Schema(example = "What is Spring Boot?")
        String title,
        @Schema(example = "600")
        Long duration,
        @Schema(example = "VIDEO")
        LessonType lessonType,
        @Schema(example = "1")
        Integer orderIndex,
        @Schema(example = "true")
        Boolean isPreview,
        @Schema(example = "[\"les_122\"]")
        List<String> prerequisiteIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        @Schema(description = "Real length of the lesson's video, in seconds; set on published curriculum reads for video lessons only", example = "425")
        Integer videoDurationSeconds
) {
    public LessonResponse withVideoDurationSeconds(Integer seconds) {
        return new LessonResponse(id, title, duration, lessonType, orderIndex, isPreview, prerequisiteIds, createdAt, updatedAt, seconds);
    }
}

