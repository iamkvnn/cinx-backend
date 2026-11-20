package com.cinx.commerce.catalog.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_policy_version")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingPolicyVersionJpaEntity {
    @Id private String id;
    @Column(nullable = false) private String code;
    @Column(name = "version_number", nullable = false) private long versionNumber;
    @Column(name = "publication_status", nullable = false) private String publicationStatus;
    @Column(name = "trial_enabled", nullable = false) private boolean trialEnabled;
    @Column(name = "renewal_failure_policy", nullable = false) private String renewalFailurePolicy;
    @Column(name = "grace_duration_seconds", nullable = false) private long graceDurationSeconds;
    @Column(name = "access_during_grace", nullable = false) private boolean accessDuringGrace;
    @Column(name = "quota_during_grace_policy", nullable = false) private String quotaDuringGracePolicy;
    @Column(name = "quota_rollover_enabled", nullable = false) private boolean quotaRolloverEnabled;
    @Column(name = "quota_overage_enabled", nullable = false) private boolean quotaOverageEnabled;
    @Column(name = "upgrade_effective_policy", nullable = false) private String upgradeEffectivePolicy;
    @Column(name = "upgrade_proration_policy", nullable = false) private String upgradeProrationPolicy;
    @Column(name = "upgrade_pending_update_required", nullable = false) private boolean upgradePendingUpdateRequired;
    @Column(name = "downgrade_effective_policy", nullable = false) private String downgradeEffectivePolicy;
    @Column(name = "cancellation_effective_policy", nullable = false) private String cancellationEffectivePolicy;
    @Column(name = "eligibility_loss_access_policy", nullable = false) private String eligibilityLossAccessPolicy;
    @Column(name = "eligibility_loss_provider_policy", nullable = false) private String eligibilityLossProviderPolicy;
    @Column(name = "eligibility_loss_refund_policy", nullable = false) private String eligibilityLossRefundPolicy;
    private LocalDateTime publishedAt;
}
