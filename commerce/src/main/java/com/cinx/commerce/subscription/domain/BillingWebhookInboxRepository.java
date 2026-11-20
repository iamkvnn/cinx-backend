package com.cinx.commerce.subscription.domain;

/** Port: where the webhook events received from billing providers are recorded. */
public interface BillingWebhookInboxRepository {
    boolean exists(String provider, String providerAccountKey, String providerEventId);

    /** Records the event, written out at once; false when the same event was already recorded, even by a racing caller. */
    boolean insertIfAbsent(BillingWebhookInbox record);

    BillingWebhookInbox save(BillingWebhookInbox record);
}
