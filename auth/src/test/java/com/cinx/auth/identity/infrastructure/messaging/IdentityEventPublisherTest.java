package com.cinx.auth.identity.infrastructure.messaging;

import com.cinx.auth.identity.application.AuthorizationCatalog;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityStatus;

import com.cinx.auth.shared.infrastructure.messaging.OutboxEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityEventPublisherTest {
    @Test
    void enqueuesVersionThreeSnapshotOrderedByChangedAt() {
        OutboxEventPublisher outbox = mock(OutboxEventPublisher.class);
        AuthorizationCatalog catalog = mock(AuthorizationCatalog.class);
        when(catalog.effectivePermissions(Set.of(AuthorizationRole.LEARNER)))
                .thenReturn(Set.of("user.profile.read.self"));
        IdentityEventPublisher publisher = new IdentityEventPublisher(outbox, catalog);
        IdentityAccount account = IdentityAccount.builder().userId("user-1").status(IdentityStatus.ACTIVE)
                .changedAt(LocalDateTime.of(2026, 8, 1, 0, 0, 0, 500_000_000)).build();
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);

        publisher.authorizationChanged(account, Set.of(AuthorizationRole.LEARNER));

        verify(outbox).enqueue(anyString(), eq("IdentityAccount"), eq("user-1"),
                eq("AuthAuthorizationChanged"), eq(3), eq("auth.events.exchange"),
                eq("auth.authorization.changed"), payload.capture(), eq(Map.of()));
        assertThat(payload.getValue()).containsEntry("changedAt", "2026-08-01T00:00:00.500Z")
                .containsEntry("status", "ACTIVE")
                .containsEntry("roles", List.of("LEARNER"))
                .containsEntry("permissions", List.of("user.profile.read.self"))
                .doesNotContainKeys("authorizationVersion", "authorizationPolicyVersion");
    }
}
