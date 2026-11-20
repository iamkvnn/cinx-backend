package com.cinx.commerce.subscription.application.query;

import java.time.LocalDateTime;

/** A subscription as its owner or an administrator reads it. */
public record SubscriptionView(String subscriptionId, String planCode, String billingStatus, String accessStatus,
                               LocalDateTime currentPeriodEnd, boolean cancelAtPeriodEnd, long entitlementVersion) {
}
