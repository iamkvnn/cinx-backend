package com.cinx.commerce.subscription.domain;

/** Port: where the staff adjustments of subscriptions are recorded. */
public interface SubscriptionAdjustmentRepository {
    SubscriptionAdjustment save(SubscriptionAdjustment adjustment);
}
