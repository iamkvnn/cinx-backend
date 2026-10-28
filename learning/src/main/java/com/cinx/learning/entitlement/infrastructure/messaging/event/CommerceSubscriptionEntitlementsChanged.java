package com.cinx.learning.entitlement.infrastructure.messaging.event;

import java.time.Instant;
import java.util.List;

public record CommerceSubscriptionEntitlementsChanged(
        String eventId,
        String subjectType,
        String subjectId,
        String userId,
        long entitlementVersion,
        String accessStatus,
        Instant effectiveFrom,
        Instant accessEffectiveUntil,
        Instant reconcileBy,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        List<Source> sources,
        List<Feature> features,
        Instant occurredAt) {
    public record Source(String subscriptionId, long subscriptionAggregateVersion, String subscriptionSlot,
                         String productLine, String planCode, long planVersion) {}

    public record Feature(String key, String type, Boolean enabled, Long limit, Boolean unlimited,
                           String unit, String resetPolicy, Instant periodStart, Instant periodEnd,
                           Instant effectiveUntil) {}
}
