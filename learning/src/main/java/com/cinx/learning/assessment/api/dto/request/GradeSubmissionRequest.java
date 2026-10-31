package com.cinx.learning.assessment.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GradeSubmissionRequest(
        @NotNull @Size(max = 20) @Valid List<CriterionScoreInput> criterionScores,
        @NotNull @Size(max = 10) @Valid List<AdjustmentInput> adjustments,
        @NotNull Double total,
        @Size(max = 5000) String feedback,
        @NotNull Long expectedVersion,
        // true when the instructor started from the AI grading suggestion; the learner is told
        Boolean aiAssisted
) {
    public record CriterionScoreInput(
            @NotBlank String criterionId,
            @NotNull @DecimalMin("0") @DecimalMax("10") Double points
    ) {
    }

    public record AdjustmentInput(
            @NotNull @DecimalMin("-10") @DecimalMax("10") Double points,
            @NotBlank @Size(max = 255) String reason
    ) {
    }
}
