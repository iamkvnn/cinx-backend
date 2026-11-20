package com.cinx.commerce.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingPolicyVersion {
    private String id;
    private String code;
    private long versionNumber;
    private String publicationStatus;
    private boolean trialEnabled;
    private String renewalFailurePolicy;
    private long graceDurationSeconds;
    private boolean accessDuringGrace;
    private String quotaDuringGracePolicy;
    private boolean quotaRolloverEnabled;
    private boolean quotaOverageEnabled;
    private String upgradeEffectivePolicy;
    private String upgradeProrationPolicy;
    private boolean upgradePendingUpdateRequired;
    private String downgradeEffectivePolicy;
    private String cancellationEffectivePolicy;
    private String eligibilityLossAccessPolicy;
    private String eligibilityLossProviderPolicy;
    private String eligibilityLossRefundPolicy;
    private LocalDateTime publishedAt;
}
