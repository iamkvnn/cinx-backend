package com.cinx.learning.assessment.api.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;


public record ChooseQuizAnswerRequest(
        @Schema(example = "q_123")
        @NotBlank(message = "questionId must not be blank")
        String questionId,

        @Schema(example = "opt_123")
        @NotBlank(message = "userAnswer must not be blank")
        String userAnswer
) {
}
