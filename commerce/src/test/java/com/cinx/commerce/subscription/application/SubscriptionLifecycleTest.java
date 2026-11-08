package com.cinx.commerce.subscription.application;

import com.cinx.commerce.subscription.domain.SubscriptionBillingStatus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionLifecycleTest {
    @Test
    void allowsPaidActivationAndGraceRecovery() {
        assertThat(SubscriptionLifecycle.canTransition(SubscriptionBillingStatus.PENDING, SubscriptionBillingStatus.ACTIVE)).isTrue();
        assertThat(SubscriptionLifecycle.canTransition(SubscriptionBillingStatus.GRACE, SubscriptionBillingStatus.ACTIVE)).isTrue();
    }

    @Test
    void terminalSubscriptionCannotResume() {
        assertThat(SubscriptionLifecycle.canTransition(SubscriptionBillingStatus.ENDED, SubscriptionBillingStatus.ACTIVE)).isFalse();
        assertThatThrownBy(() -> SubscriptionLifecycle.requireTransition(SubscriptionBillingStatus.ENDED, SubscriptionBillingStatus.ACTIVE))
                .isInstanceOf(IllegalStateException.class);
    }
}
