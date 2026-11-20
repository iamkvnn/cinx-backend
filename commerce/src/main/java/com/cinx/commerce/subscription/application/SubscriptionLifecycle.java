package com.cinx.commerce.subscription.application;

import com.cinx.commerce.subscription.domain.SubscriptionBillingStatus;

import java.util.EnumSet;
import java.util.Map;

public final class SubscriptionLifecycle {
    private static final Map<SubscriptionBillingStatus, EnumSet<SubscriptionBillingStatus>> ALLOWED = Map.of(
            SubscriptionBillingStatus.PENDING, EnumSet.of(SubscriptionBillingStatus.ACTIVE, SubscriptionBillingStatus.ENDED, SubscriptionBillingStatus.INELIGIBLE),
            SubscriptionBillingStatus.TRIALING, EnumSet.of(SubscriptionBillingStatus.ACTIVE, SubscriptionBillingStatus.PAST_DUE, SubscriptionBillingStatus.CANCELED, SubscriptionBillingStatus.INELIGIBLE),
            SubscriptionBillingStatus.ACTIVE, EnumSet.of(SubscriptionBillingStatus.PAST_DUE, SubscriptionBillingStatus.GRACE, SubscriptionBillingStatus.CANCELED, SubscriptionBillingStatus.INELIGIBLE, SubscriptionBillingStatus.ENDED),
            SubscriptionBillingStatus.PAST_DUE, EnumSet.of(SubscriptionBillingStatus.ACTIVE, SubscriptionBillingStatus.GRACE, SubscriptionBillingStatus.CANCELED, SubscriptionBillingStatus.ENDED, SubscriptionBillingStatus.INELIGIBLE),
            SubscriptionBillingStatus.GRACE, EnumSet.of(SubscriptionBillingStatus.ACTIVE, SubscriptionBillingStatus.CANCELED, SubscriptionBillingStatus.ENDED, SubscriptionBillingStatus.INELIGIBLE),
            SubscriptionBillingStatus.CANCELED, EnumSet.of(SubscriptionBillingStatus.ENDED),
            SubscriptionBillingStatus.INELIGIBLE, EnumSet.of(SubscriptionBillingStatus.ENDED, SubscriptionBillingStatus.ACTIVE),
            SubscriptionBillingStatus.ENDED, EnumSet.noneOf(SubscriptionBillingStatus.class));

    private SubscriptionLifecycle() {
    }

    public static boolean canTransition(SubscriptionBillingStatus from, SubscriptionBillingStatus to) {
        return from != null && to != null && ALLOWED.getOrDefault(from, EnumSet.noneOf(SubscriptionBillingStatus.class)).contains(to);
    }

    public static void requireTransition(SubscriptionBillingStatus from, SubscriptionBillingStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Subscription transition is invalid: " + from + " -> " + to);
        }
    }
}
