package com.cinx.learning.learningpath.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathItemRequest {
    @Schema(example = "course_123")
    @NotBlank(message = "courseId must not be blank")
    private String courseId;
    @Schema(example = "lesson_123")
    @NotBlank(message = "lessonId must not be blank")
    private String lessonId;
    @Schema(example = "1")
    private Integer orderIndex;
    @Schema(description = "The named phase this lesson belongs to; consecutive lessons share one", example = "Nền tảng React")
    @Size(max = 60, message = "phase must be at most 60 characters")
    private String phase;
    @Schema(example = "true")
    private Boolean isSuggested;
}
