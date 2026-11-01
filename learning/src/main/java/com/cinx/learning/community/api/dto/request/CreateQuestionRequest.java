package com.cinx.learning.community.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateQuestionRequest {
    @NotBlank
    @Schema(example = "course_123")
    private String courseId;

    @Schema(example = "les_123")
    private String lessonId;

    @NotBlank
    @Schema(example = "What is polymorphism?")
    private String title;

    @NotBlank
    @Schema(example = "Could you give a real-world example of polymorphism?")
    private String content;
}
