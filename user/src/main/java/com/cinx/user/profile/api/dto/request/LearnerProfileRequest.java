package com.cinx.user.profile.api.dto.request;

import com.cinx.user.profile.domain.ContentLanguage;
import com.cinx.user.profile.domain.LearnerLevel;
import com.cinx.user.profile.domain.LearningGoal;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "The whole learning profile; a field left out is cleared")
public record LearnerProfileRequest(
        @Schema(example = "SKILL_UP")
        LearningGoal goal,
        @Schema(example = "BASIC")
        LearnerLevel level,
        @Schema(example = "45", description = "15, 30, 45, 60, 90 or 120")
        Integer minutesPerDay,
        @Schema(example = "BOTH")
        ContentLanguage contentLanguage,
        @Size(max = 20)
        List<@NotBlank String> categoryIds,
        @Schema(description = "Whether cinx may use this profile to personalize; left out keeps the stored choice")
        Boolean personalizationConsent,
        @Schema(description = "Whether product events about this learner may be recorded; left out keeps the stored choice")
        Boolean usageAnalyticsConsent
) {
}
