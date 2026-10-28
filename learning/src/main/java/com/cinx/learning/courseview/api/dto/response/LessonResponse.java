package com.cinx.learning.courseview.api.dto.response;

import com.cinx.learning.shared.domain.LessonType;
import io.swagger.v3.oas.annotations.media.Schema;

public record LessonResponse (
        @Schema(example = "les_123")
        String id,
        @Schema(example = "What is Java?")
        String title,
        @Schema(example = "1200")
        Long duration,
        @Schema(example = "VIDEO")
        LessonType lessonType,
        @Schema(example = "1")
        Integer orderIndex
) {
}
