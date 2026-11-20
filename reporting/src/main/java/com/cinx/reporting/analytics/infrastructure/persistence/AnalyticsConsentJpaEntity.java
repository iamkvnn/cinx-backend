package com.cinx.reporting.analytics.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "analytics_consent")
@Getter
@Setter
@NoArgsConstructor
class AnalyticsConsentJpaEntity {
    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
