package com.cinx.learning.assessment.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record QuizQuestionAnalyticsResponse(
    @Schema(example = "q_123")
    String questionId,
    @Schema(example = "100")
    Integer totalAttempts,
    @Schema(example = "80")
    Integer correctAttempts,
    @Schema(example = "0.8")
    Double accuracy
) {}