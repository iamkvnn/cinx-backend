package com.cinx.learning.assessment.application.command;

import java.util.List;

public record GradeSubmissionCommand(
           List<CriterionScoreInput> criterionScores,
           List<AdjustmentInput> adjustments,
         Double total,
         String feedback,
         Long expectedVersion,
         Boolean aiAssisted
) {
    public record CriterionScoreInput(
             String criterionId,
               Double points
    ) {
    }

    public record AdjustmentInput(
               Double points,
              String reason
    ) {
    }
}
