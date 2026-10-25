package com.cinx.user.profile.application.query;

import com.cinx.user.profile.domain.ContentLanguage;
import com.cinx.user.profile.domain.LearnerLevel;
import com.cinx.user.profile.domain.LearnerProfile;
import com.cinx.user.profile.domain.LearnerProfileStatus;
import com.cinx.user.profile.domain.LearningGoal;

import java.time.LocalDateTime;
import java.util.List;

public record LearnerProfileView(
        LearningGoal goal,
        LearnerLevel level,
        Integer minutesPerDay,
        ContentLanguage contentLanguage,
        List<String> categoryIds,
        boolean personalizationConsent,
        boolean usageAnalyticsConsent,
        LearnerProfileStatus status,
        LocalDateTime updatedAt) {

    public static LearnerProfileView of(LearnerProfile profile) {
        return new LearnerProfileView(profile.getGoal(), profile.getLevel(), profile.getMinutesPerDay(),
                profile.getContentLanguage(), profile.getCategoryIds(), profile.isPersonalizationConsent(),
                profile.isUsageAnalyticsConsent(), profile.getStatus(), profile.getUpdatedAt());
    }
}
