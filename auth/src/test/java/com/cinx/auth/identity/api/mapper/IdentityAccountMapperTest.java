package com.cinx.auth.identity.api.mapper;

import com.cinx.auth.identity.api.dto.LinkIdentityRequest;
import com.cinx.auth.identity.api.dto.UpdateRolesRequest;
import com.cinx.auth.identity.api.dto.request.BanUserRequest;
import com.cinx.auth.identity.application.query.AuthorizationState;
import com.cinx.auth.identity.application.query.IdentityAccountView;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.BanReasonType;
import com.cinx.auth.identity.domain.IdentityStatus;
import com.cinx.auth.identity.domain.IdentitySyncStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityAccountMapperTest {
    private final IdentityAccountMapper mapper = Mappers.getMapper(IdentityAccountMapper.class);

    @Test
    void theAccountResponseKeepsEveryField() {
        LocalDateTime until = LocalDateTime.of(2026, 9, 1, 0, 0);

        var response = mapper.toDto(new IdentityAccountView("user-1", "kc-1", "u@example.com",
                IdentityStatus.BANNED, IdentitySyncStatus.PENDING, until));

        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.keycloakSubject()).isEqualTo("kc-1");
        assertThat(response.email()).isEqualTo("u@example.com");
        assertThat(response.status()).isEqualTo(IdentityStatus.BANNED);
        assertThat(response.syncStatus()).isEqualTo(IdentitySyncStatus.PENDING);
        assertThat(response.banExpiresAt()).isEqualTo(until);
    }

    @Test
    void theAuthorizationStateResponseKeepsTheVersionsAndTheRoleNames() {
        Instant at = Instant.parse("2026-08-01T00:00:00Z");

        var response = mapper.toDto(new AuthorizationState("user-1", IdentityStatus.ACTIVE, List.of("LEARNER"), 7L, 2L, at));

        assertThat(response.roles()).containsExactly("LEARNER");
        assertThat(response.authorizationVersion()).isEqualTo(7L);
        assertThat(response.authorizationPolicyVersion()).isEqualTo(2L);
        assertThat(response.evaluatedAt()).isEqualTo(at);
    }

    @Test
    void theRequestsBecomeCommandsWithTheSameValues() {
        assertThat(mapper.toCommand(new LinkIdentityRequest("kc-1", "u@example.com")))
                .satisfies(command -> {
                    assertThat(command.keycloakSubject()).isEqualTo("kc-1");
                    assertThat(command.email()).isEqualTo("u@example.com");
                });
        assertThat(mapper.toCommand(new BanUserRequest(BanReasonType.SPAM, "links", 30)))
                .satisfies(command -> {
                    assertThat(command.reasonType()).isEqualTo(BanReasonType.SPAM);
                    assertThat(command.details()).isEqualTo("links");
                    assertThat(command.durationDays()).isEqualTo(30);
                });
        Instant end = Instant.parse("2026-12-01T00:00:00Z");
        assertThat(mapper.toCommand(new UpdateRolesRequest(Set.of(AuthorizationRole.SUPPORT_AGENT), "audit", end)))
                .satisfies(command -> {
                    assertThat(command.roles()).containsExactly(AuthorizationRole.SUPPORT_AGENT);
                    assertThat(command.expiresAt()).isEqualTo(end);
                });
    }
}
