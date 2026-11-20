package com.cinx.learning.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MoveLessonRequest(
        @NotBlank
        String targetSectionId,
        String previousLessonId,
        String nextLessonId
) {
}

