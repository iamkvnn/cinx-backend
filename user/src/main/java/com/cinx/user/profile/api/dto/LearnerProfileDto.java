package com.cinx.user.profile.api.dto;

import com.cinx.user.profile.domain.ContentLanguage;
import com.cinx.user.profile.domain.LearnerLevel;
import com.cinx.user.profile.domain.LearnerProfileStatus;
import com.cinx.user.profile.domain.LearningGoal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record LearnerProfileDto(
        LearningGoal goal,
        LearnerLevel level,
        Integer minutesPerDay,
        ContentLanguage contentLanguage,
        List<String> categoryIds,
        boolean personalizationConsent,
        @Schema(description = "Whether product events about this learner may be recorded (\"Thống kê sử dụng\")")
        boolean usageAnalyticsConsent,
        @Schema(description = "NONE until the learner answers or skips the profile questions")
        LearnerProfileStatus status,
        LocalDateTime updatedAt
) {
}
