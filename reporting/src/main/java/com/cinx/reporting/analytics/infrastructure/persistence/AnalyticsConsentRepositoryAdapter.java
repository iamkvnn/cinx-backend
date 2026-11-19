package com.cinx.reporting.analytics.infrastructure.persistence;

import com.cinx.reporting.analytics.domain.AnalyticsConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
class AnalyticsConsentRepositoryAdapter implements AnalyticsConsentRepository {
    private final AnalyticsConsentJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public boolean allows(String userId) {
        return jpa.findById(userId).map(AnalyticsConsentJpaEntity::isEnabled).orElse(true);
    }

    @Override
    @Transactional
    public void record(String userId, boolean enabled, LocalDateTime updatedAt) {
        AnalyticsConsentJpaEntity row = jpa.findById(userId).orElseGet(AnalyticsConsentJpaEntity::new);
        // events may arrive out of order; an older snapshot never undoes a newer switch
        if (row.getUpdatedAt() != null && row.getUpdatedAt().isAfter(updatedAt)) {
            return;
        }
        row.setUserId(userId);
        row.setEnabled(enabled);
        row.setUpdatedAt(updatedAt);
        jpa.save(row);
    }
}
