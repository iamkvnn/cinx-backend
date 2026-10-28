package com.cinx.learning.courseview.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SectionResponse(
        @Schema(example = "sec_123")
        String id,
        @Schema(example = "Introduction to Java")
        String title,
        @Schema(example = "Learn the basics of Java programming")
        String description,
        @Schema(example = "3600")
        Long duration,
        @Schema(example = "1")
        Integer orderIndex,
        List<LessonResponse> lessons
) {
}
