package com.cinx.learning.review.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewAnswerRequest(
        @Size(max = 10000)
        @Schema(description = "The answer encoded as a quiz session's choose call encodes it for the question type")
        String userAnswer,
        @Min(0) @Max(86400)
        @Schema(example = "25", description = "Seconds spent on the question; at most 300 count as learning activity")
        Integer elapsedSeconds
) {
}
