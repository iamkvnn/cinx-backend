package com.cinx.learning.entitlement.application;

import java.time.LocalDateTime;

/** What the other features ask of the subscription entitlement projection that commerce keeps current. */
public interface SubscriptionFeatureAccess {
    /** Whether the learner's subscription is active (or in grace) at {@code now} and grants the feature. */
    boolean hasFeature(String userId, String featureKey, LocalDateTime now);
}
