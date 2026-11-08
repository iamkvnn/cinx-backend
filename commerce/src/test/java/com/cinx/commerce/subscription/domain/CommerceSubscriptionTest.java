package com.cinx.commerce.subscription.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommerceSubscriptionTest {
    @Test
    void theBusinessVersionMovesOnByOne() {
        CommerceSubscription subscription = CommerceSubscription.builder().aggregateVersion(3L).build();

        subscription.advanceVersion();

        assertThat(subscription.getAggregateVersion()).isEqualTo(4L);
    }

    @Test
    void aVersionBelowOneIsLiftedToOneNotToTwo() {
        CommerceSubscription fresh = CommerceSubscription.builder().aggregateVersion(0L).build();

        fresh.advanceVersion();

        assertThat(fresh.getAggregateVersion()).isEqualTo(1L);
    }
}
