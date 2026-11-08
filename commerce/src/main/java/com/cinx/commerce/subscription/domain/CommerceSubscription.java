package com.cinx.commerce.subscription.domain;

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
public class CommerceSubscription {
    private String id;
    private String subjectId;
    private String planVersionId;
    private String priceId;
    private String assignmentBindingId;
    private String origin;
    private String provider;
    private String providerAccountKey;
    private String providerCustomerRef;
    private String providerSubscriptionRef;
    private String billingStatus;
    private String accessStatus;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime trialStartedAt;
    private LocalDateTime trialEndsAt;
    private LocalDateTime graceStartedAt;
    private LocalDateTime graceEndsAt;
    private boolean cancelAtPeriodEnd;
    private LocalDateTime canceledAt;
    private LocalDateTime endedAt;
    private long providerObjectVersion;
    private long aggregateVersion;
    private long optimisticVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Moves the business version of the subscription on by one; it is never below 1. */
    public void advanceVersion() {
        aggregateVersion = Math.max(1L, aggregateVersion + 1);
    }
}
