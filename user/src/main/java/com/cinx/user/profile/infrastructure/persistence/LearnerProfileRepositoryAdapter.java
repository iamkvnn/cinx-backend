package com.cinx.user.profile.infrastructure.persistence;

import com.cinx.user.profile.domain.LearnerProfile;
import com.cinx.user.profile.domain.LearnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class LearnerProfileRepositoryAdapter implements LearnerProfileRepository {
    private final LearnerProfileJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public Optional<LearnerProfile> findByUserId(String userId) {
        return jpa.findById(userId).map(LearnerProfileRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public LearnerProfile save(LearnerProfile profile) {
        LearnerProfileJpaEntity row = jpa.findById(profile.getUserId()).orElseGet(LearnerProfileJpaEntity::new);
        row.setUserId(profile.getUserId());
        row.setGoal(profile.getGoal());
        row.setLevel(profile.getLevel());
        row.setMinutesPerDay(profile.getMinutesPerDay());
        row.setContentLanguage(profile.getContentLanguage());
        row.setPersonalizationConsent(profile.isPersonalizationConsent());
        row.setUsageAnalyticsConsent(profile.isUsageAnalyticsConsent());
        row.setStatus(profile.getStatus());
        row.setUpdatedAt(profile.getUpdatedAt());
        // the managed set is edited in place, so Hibernate writes only the topics that changed
        row.getCategoryIds().retainAll(profile.getCategoryIds());
        row.getCategoryIds().addAll(profile.getCategoryIds());
        return toDomain(jpa.save(row));
    }

    private static LearnerProfile toDomain(LearnerProfileJpaEntity row) {
        return LearnerProfile.builder()
                .userId(row.getUserId())
                .goal(row.getGoal())
                .level(row.getLevel())
                .minutesPerDay(row.getMinutesPerDay())
                .contentLanguage(row.getContentLanguage())
                .categoryIds(List.copyOf(row.getCategoryIds()))
                .personalizationConsent(row.isPersonalizationConsent())
                .usageAnalyticsConsent(row.isUsageAnalyticsConsent())
                .status(row.getStatus())
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}
