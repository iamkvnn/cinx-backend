package com.cinx.commerce.subscription.application;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingAvailabilityService implements BillingAvailability {
    private final ObjectProvider<BillingProvider> billingProvider;

    @Value("${cinx.subscription.billing-enabled:false}")
    private boolean billingEnabled;

    @Override
    public boolean available() {
        return billingEnabled && billingProvider.getIfAvailable() != null;
    }
}
