package com.cinx.user.profile.application.command;

import com.cinx.user.profile.domain.ContentLanguage;
import com.cinx.user.profile.domain.LearnerLevel;
import com.cinx.user.profile.domain.LearningGoal;

import java.util.List;

/**
 * The whole profile from the "Hồ sơ học tập" tab; a missing answer clears it.
 *
 * @param personalizationConsent null keeps the stored choice (on for a new profile)
 * @param usageAnalyticsConsent null keeps the stored choice (on for a new profile)
 */
public record SaveLearnerProfileCommand(
        LearningGoal goal,
        LearnerLevel level,
        Integer minutesPerDay,
        ContentLanguage contentLanguage,
        List<String> categoryIds,
        Boolean personalizationConsent,
        Boolean usageAnalyticsConsent) {
}
