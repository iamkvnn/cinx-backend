package com.cinx.user.profile.api.dto.request;

import com.cinx.user.profile.domain.ContentLanguage;
import com.cinx.user.profile.domain.LearnerLevel;
import com.cinx.user.profile.domain.LearningGoal;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The onboarding step \"Mục tiêu học\"; send it empty when the learner skipped the step")
public record LearnerProfileAnswersRequest(
        @Schema(example = "CAREER_START")
        LearningGoal goal,
        @Schema(example = "BEGINNER")
        LearnerLevel level,
        @Schema(example = "30", description = "15, 30, 45, 60, 90 or 120")
        Integer minutesPerDay,
        @Schema(example = "VI")
        ContentLanguage contentLanguage
) {
}
