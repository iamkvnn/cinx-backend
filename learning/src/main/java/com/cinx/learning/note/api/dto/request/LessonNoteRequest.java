package com.cinx.learning.note.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create or update a lesson note; positionSeconds only for video lessons. */
public record LessonNoteRequest(
        @NotBlank
        @Size(max = 5000)
        @Schema(example = "ResponseEntityExceptionHandler handles MVC errors already")
        String content,
        @Min(0)
        @Schema(description = "Seconds into the video; omit for non-video lessons", example = "151")
        Integer positionSeconds
) {
}
