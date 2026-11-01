package com.cinx.learning.community.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAnswerRequest {
    @NotBlank(message = "questionId must not be blank")
    @Schema(example = "q_123")
    private String questionId;

    @Schema(example = "ans_098")
    private String parentAnswerId;

    @NotBlank(message = "content must not be blank")
    @Schema(example = "Polymorphism allows methods to do different things based on the object.")
    private String content;

    @Schema(description = "true when the author started from an AI draft; the learner is told", example = "false")
    private Boolean aiAssisted;
}
