package com.cinx.commerce.subscription.infrastructure.provider;

import com.cinx.commerce.subscription.application.BillingProvider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Deterministic provider for local/test only; never selectable in production. */
@Component
@Profile({"local", "test"})
@ConditionalOnProperty(name = "cinx.subscription.provider", havingValue = "FAKE")
public class LocalFakeBillingProvider implements BillingProvider {
    private final Clock clock;

    public LocalFakeBillingProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public CheckoutSession createSubscriptionCheckout(CheckoutCommand command) {
        return new CheckoutSession("fake_session_" + command.commandId(),
                "http://localhost:5174" + command.successPath(), Instant.now(clock).plus(Duration.ofMinutes(15)));
    }

    @Override
    public ChangeQuote previewChange(ChangeCommand command) {
        return new ChangeQuote(java.math.BigDecimal.ZERO, "VND", Instant.now(clock).plus(Duration.ofMinutes(5)), command.commandId());
    }

    @Override
    public ProviderSubscription changeSubscription(ChangeCommand command) {
        return subscription(command.providerSubscriptionRef());
    }

    @Override
    public ProviderSubscription cancelAtPeriodEnd(String providerSubscriptionRef) {
        ProviderSubscription value = subscription(providerSubscriptionRef);
        return new ProviderSubscription(value.reference(), "ACTIVE", value.periodStart(), value.periodEnd(), true, value.providerVersion());
    }

    @Override
    public ProviderSubscription resume(String providerSubscriptionRef) {
        return subscription(providerSubscriptionRef);
    }

    @Override
    public ProviderSubscription fetchSubscription(String providerSubscriptionRef) {
        return subscription(providerSubscriptionRef);
    }

    @Override
    public NormalizedWebhook verifyAndParseWebhook(byte[] rawBody, ProviderHeaders headers) {
        throw new UnsupportedOperationException("Fake provider does not accept external webhooks");
    }

    private ProviderSubscription subscription(String reference) {
        Instant now = Instant.now(clock);
        return new ProviderSubscription(reference, "ACTIVE", now, now.plus(Duration.ofDays(30)), false, 1);
    }
}
