package com.cinx.learning.catalog.api.dto.request;

import com.cinx.learning.catalog.domain.ReviewNoteTarget;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A reviewer's note on one place of a course; lessonId is the lesson's stable id and required for LESSON. */
public record ReviewNoteRequest(
        @NotNull ReviewNoteTarget target,
        String lessonId,
        @Min(0) Integer videoSecond,
        @NotBlank @Size(max = 2000) String content
) {
}
