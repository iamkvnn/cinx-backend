package com.cinx.auth.identity.api.mapper;

import com.cinx.auth.identity.api.dto.UpdateRolesRequest;
import com.cinx.auth.identity.api.dto.request.BanUserRequest;
import com.cinx.auth.identity.application.query.AuthorizationState;
import com.cinx.auth.identity.application.query.IdentityAccountView;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.BanReasonType;
import com.cinx.auth.identity.domain.IdentityStatus;
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
        LocalDateTime changed = LocalDateTime.of(2026, 8, 1, 0, 0);

        var response = mapper.toDto(new IdentityAccountView("user-1", "u@example.com", IdentityStatus.BANNED,
                List.of("LEARNER"), until, changed));

        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.email()).isEqualTo("u@example.com");
        assertThat(response.status()).isEqualTo(IdentityStatus.BANNED);
        assertThat(response.roles()).containsExactly("LEARNER");
        assertThat(response.banExpiresAt()).isEqualTo(until);
        assertThat(response.changedAt()).isEqualTo(changed);
    }

    @Test
    void theAuthorizationStateResponseKeepsTheLastChange() {
        Instant changed = Instant.parse("2026-08-01T00:00:00Z");
        Instant at = Instant.parse("2026-08-02T00:00:00Z");

        var response = mapper.toDto(new AuthorizationState("user-1", IdentityStatus.ACTIVE, changed, at));

        assertThat(response.status()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(response.changedAt()).isEqualTo(changed);
        assertThat(response.evaluatedAt()).isEqualTo(at);
    }

    @Test
    void theRequestsBecomeCommandsWithTheSameValues() {
        assertThat(mapper.toCommand(new BanUserRequest(BanReasonType.SPAM, "links", 30)))
                .satisfies(command -> {
                    assertThat(command.reasonType()).isEqualTo(BanReasonType.SPAM);
                    assertThat(command.details()).isEqualTo("links");
                    assertThat(command.durationDays()).isEqualTo(30);
                });
        assertThat(mapper.toCommand(new UpdateRolesRequest(Set.of(AuthorizationRole.SUPPORT_AGENT), "audit")))
                .satisfies(command -> assertThat(command.roles()).containsExactly(AuthorizationRole.SUPPORT_AGENT));
    }
}
