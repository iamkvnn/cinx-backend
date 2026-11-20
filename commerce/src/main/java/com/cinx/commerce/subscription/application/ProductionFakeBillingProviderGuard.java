package com.cinx.commerce.subscription.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Fails closed if a production-like profile attempts to select the fake adapter. */
@Component
@Profile("!local & !test")
@ConditionalOnProperty(name = "cinx.subscription.provider", havingValue = "FAKE")
public class ProductionFakeBillingProviderGuard {
    public ProductionFakeBillingProviderGuard() {
        throw new IllegalStateException("FAKE billing provider is only allowed in local/test profiles");
    }
}
