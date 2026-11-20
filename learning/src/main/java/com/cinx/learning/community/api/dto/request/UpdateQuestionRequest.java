package com.cinx.learning.community.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class UpdateQuestionRequest {
    @NotBlank(message = "title must not be blank")
    @Schema(example = "Updated question title")
    private String title;

    @NotBlank(message = "content must not be blank")
    @Schema(example = "Updated question content")
    private String content;
}
