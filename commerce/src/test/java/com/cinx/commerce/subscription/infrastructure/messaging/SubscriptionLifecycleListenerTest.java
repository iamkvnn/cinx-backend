package com.cinx.commerce.subscription.infrastructure.messaging;

import com.cinx.commerce.subscription.application.SubscriptionProvisioningService;
import com.cinx.commerce.subscription.infrastructure.persistence.AuthEligibilityEventInboxRepository;
import com.cinx.commerce.subscription.infrastructure.persistence.AuthEligibilityProjection;
import com.cinx.commerce.subscription.infrastructure.persistence.AuthEligibilityProjectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionLifecycleListenerTest {
    private final AuthEligibilityEventInboxRepository inbox = mock(AuthEligibilityEventInboxRepository.class);
    private final AuthEligibilityProjectionRepository projections = mock(AuthEligibilityProjectionRepository.class);
    private final SubscriptionProvisioningService provisioning = mock(SubscriptionProvisioningService.class);
    private SubscriptionLifecycleListener listener;

    @BeforeEach
    void setUp() {
        listener = new SubscriptionLifecycleListener(inbox, projections, provisioning, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-02T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void anOlderChangeThanTheProjectionIsIgnored() {
        AuthEligibilityProjection current = AuthEligibilityProjection.builder().userId("user-1").accountStatus("ACTIVE")
                .rolesJson("[\"LEARNER\"]").changedAt(LocalDateTime.of(2026, 8, 1, 0, 0)).build();
        when(projections.findById("user-1")).thenReturn(Optional.of(current));

        listener.onAuthorizationChanged(event("BANNED", "2026-07-31T23:59:59Z"), "auth.authorization.changed", "e1");

        verify(projections, never()).save(any());
        verify(provisioning, never()).applyAuthorization(anyString(), anyString(), anyList());
    }

    @Test
    void aLaterChangeUpdatesTheProjectionAndTheSubscription() {
        when(projections.findById("user-1")).thenReturn(Optional.empty());

        listener.onAuthorizationChanged(event("BANNED", "2026-08-01T00:00:00.500Z"), "auth.authorization.changed", "e2");

        verify(projections).save(any());
        verify(provisioning).applyAuthorization("user-1", "BANNED", List.of("LEARNER"));
    }

    @Test
    void anEventWithoutChangedAtIsRefused() {
        assertThatThrownBy(() -> listener.onAuthorizationChanged(Map.of("userId", "user-1", "status", "ACTIVE"),
                "auth.authorization.changed", "e3")).isInstanceOf(IllegalArgumentException.class);
    }

    private static Map<String, Object> event(String status, String changedAt) {
        return Map.of("userId", "user-1", "status", status, "roles", List.of("LEARNER"), "changedAt", changedAt);
    }
}
