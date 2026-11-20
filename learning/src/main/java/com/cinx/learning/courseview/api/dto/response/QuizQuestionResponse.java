package com.cinx.learning.courseview.api.dto.response;

import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.ScoringMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record QuizQuestionResponse(
        @Schema(example = "q_123")
        String id,
        @Schema(example = "What is encapsulation?")
        String questionText,
        @Schema(example = "MULTIPLE_CHOICE")
        QuizQuestionType questionType,
        @Schema(example = "PARTIAL")
        ScoringMethod scoringMethod,
        List<QuizOptionResponse> options
) {
}
