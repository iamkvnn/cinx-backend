package com.cinx.commerce.catalog.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plan_version")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriptionPlanVersionJpaEntity {
    @Id private String id;
    @Column(name = "plan_id", nullable = false) private String planId;
    @Column(name = "billing_policy_version_id", nullable = false) private String billingPolicyVersionId;
    @Column(name = "version_number", nullable = false) private long versionNumber;
    @Column(name = "publication_status", nullable = false) private String publicationStatus;
    @Column(name = "default_locale", nullable = false) private String defaultLocale;
    private LocalDateTime effectiveFrom;
    private LocalDateTime publishedAt;
    private String publishedBy;
    @Version
    @Column(name = "optimistic_version", nullable = false)
    private long optimisticVersion;
}
