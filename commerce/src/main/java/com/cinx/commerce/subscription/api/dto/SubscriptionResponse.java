package com.cinx.commerce.subscription.api.dto;

import java.time.LocalDateTime;

public record SubscriptionResponse(String subscriptionId, String planCode, String billingStatus,
                                   String accessStatus, LocalDateTime currentPeriodEnd,
                                   boolean cancelAtPeriodEnd, long entitlementVersion) {
}
