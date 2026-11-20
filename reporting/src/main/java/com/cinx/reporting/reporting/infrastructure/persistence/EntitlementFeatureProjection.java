package com.cinx.reporting.reporting.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reporting_entitlement_feature_projection",
        uniqueConstraints = @UniqueConstraint(name = "uk_reporting_entitlement_feature", columnNames = {"user_id", "feature_key"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EntitlementFeatureProjection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(name = "feature_key", nullable = false, length = 160) private String featureKey;
    @Column(nullable = false) private boolean enabled;
    private Long quotaLimit;
    @Column(nullable = false) private long entitlementVersion;
}
