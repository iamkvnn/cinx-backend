package com.cinx.auth.identity.infrastructure.messaging;

import com.cinx.auth.identity.application.AuthorizationCatalog;
import com.cinx.auth.identity.application.IdentityEvents;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.shared.infrastructure.messaging.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdentityEventPublisher implements IdentityEvents {
    private static final String EXCHANGE = "auth.events.exchange";

    private final OutboxEventPublisher outboxEventPublisher;
    private final AuthorizationCatalog catalog;

    @Override
    public void authorizationChanged(IdentityAccount account, Set<AuthorizationRole> roles) {
        outboxEventPublisher.enqueue(
                UUID.randomUUID().toString(),
                "IdentityAccount",
                account.getUserId(),
                "AuthAuthorizationChanged",
                3,
                EXCHANGE,
                "auth.authorization.changed",
                Map.of(
                        "userId", account.getUserId(),
                        "status", account.getStatus().name(),
                        "roles", roles.stream().map(Enum::name).sorted().toList(),
                        "permissions", catalog.effectivePermissions(roles).stream().sorted().toList(),
                        "changedAt", account.getChangedAt().toInstant(ZoneOffset.UTC).toString()),
                Map.of());
    }
}
