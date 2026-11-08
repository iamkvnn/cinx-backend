package com.cinx.commerce.subscription.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingAvailabilityServiceTest {
    @Mock ObjectProvider<BillingProvider> provider;

    @Test
    void billingIsAvailableOnlyWhenItIsSwitchedOnAndAProviderIsConfigured() {
        BillingAvailabilityService service = new BillingAvailabilityService(provider);
        when(provider.getIfAvailable()).thenReturn(mock(BillingProvider.class), (BillingProvider) null);

        ReflectionTestUtils.setField(service, "billingEnabled", true);
        assertThat(service.available()).as("on, provider present").isTrue();
        assertThat(service.available()).as("on, no provider").isFalse();
    }

    @Test
    void billingSwitchedOffIsNeverAvailableEvenWithAProvider() {
        BillingAvailabilityService service = new BillingAvailabilityService(provider);
        ReflectionTestUtils.setField(service, "billingEnabled", false);

        assertThat(service.available()).isFalse();
    }
}
