package com.cinx.learning.assessment.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SubmitVideoQuestionRequest(
        @Schema(example = "vass_123")
        @NotBlank(message = "videoAssessmentId must not be blank")
        String videoAssessmentId,

        @Schema(example = "opt_1")
        @NotBlank(message = "userAnswer must not be blank")
        String userAnswer
) {
}
