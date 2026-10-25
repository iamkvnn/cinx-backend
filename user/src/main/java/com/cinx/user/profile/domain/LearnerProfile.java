package com.cinx.user.profile.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** What a learner is learning for and how, one per learner; every answer may be missing. */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class LearnerProfile {
    /** The study plan's own range, in the steps the client offers. */
    public static final Set<Integer> MINUTES_PER_DAY = Set.of(15, 30, 45, 60, 90, 120);
    public static final int MAX_CATEGORIES = 20;

    private final String userId;
    private final LearningGoal goal;
    private final LearnerLevel level;
    private final Integer minutesPerDay;
    private final ContentLanguage contentLanguage;
    private final List<String> categoryIds;
    /** Whether cinx may use the profile to personalize; on until the learner switches it off. */
    private final boolean personalizationConsent;
    /** Whether product events about this learner may be recorded; on until switched off. */
    private final boolean usageAnalyticsConsent;
    private final LearnerProfileStatus status;
    private final LocalDateTime updatedAt;

    public static LearnerProfile empty(String userId) {
        return LearnerProfile.builder()
                .userId(userId)
                .categoryIds(List.of())
                .personalizationConsent(true)
                .usageAnalyticsConsent(true)
                .status(LearnerProfileStatus.NONE)
                .build();
    }
}
