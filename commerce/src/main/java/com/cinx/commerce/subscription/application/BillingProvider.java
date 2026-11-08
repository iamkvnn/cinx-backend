package com.cinx.commerce.subscription.application;

import java.math.BigDecimal;
import java.time.Instant;

/** Provider-neutral boundary; domain code never switches on provider statuses. */
public interface BillingProvider {
    CheckoutSession createSubscriptionCheckout(CheckoutCommand command);

    ChangeQuote previewChange(ChangeCommand command);

    ProviderSubscription changeSubscription(ChangeCommand command);

    ProviderSubscription cancelAtPeriodEnd(String providerSubscriptionRef);

    default ProviderSubscription cancelAtPeriodEnd(String providerSubscriptionRef, String commandId) {
        return cancelAtPeriodEnd(providerSubscriptionRef);
    }

    ProviderSubscription resume(String providerSubscriptionRef);

    default ProviderSubscription resume(String providerSubscriptionRef, String commandId) {
        return resume(providerSubscriptionRef);
    }

    ProviderSubscription fetchSubscription(String providerSubscriptionRef);

    NormalizedWebhook verifyAndParseWebhook(byte[] rawBody, ProviderHeaders headers);

    record CheckoutCommand(String commandId, String customerReference, String providerPriceReference,
                           String successPath, String cancelPath) {}

    record ChangeCommand(String commandId, String providerSubscriptionRef, String providerPriceReference) {}

    record CheckoutSession(String providerSessionReference, String checkoutUrl, Instant expiresAt) {}

    record ChangeQuote(BigDecimal amountDue, String currency, Instant expiresAt, String fingerprint) {}

    record ProviderSubscription(String reference, String status, Instant periodStart, Instant periodEnd,
                               boolean cancelAtPeriodEnd, long providerVersion) {}

    record ProviderHeaders(String signature, String timestamp, String accountKey) {}

    record NormalizedWebhook(String providerEventId, String objectReference, long objectVersion,
                             String eventType, String status, Instant occurredAt) {}
}
