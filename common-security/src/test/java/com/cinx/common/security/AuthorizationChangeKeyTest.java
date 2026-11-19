package com.cinx.common.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationChangeKeyTest {
    private static final Instant CHANGE = Instant.parse("2026-11-14T10:00:00.700Z");

    @Test
    void keyIsPrefixedByPurpose() {
        assertThat(AuthorizationChangeKey.key("user-1")).isEqualTo("authz:changed:user-1");
        assertThatThrownBy(() -> AuthorizationChangeKey.key(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void valueRoundTrips() {
        String value = AuthorizationChangeKey.encode("BANNED", CHANGE);

        assertThat(value).isEqualTo("BANNED " + CHANGE.toEpochMilli());
        assertThat(AuthorizationChangeKey.decode(value))
                .isEqualTo(new AuthorizationChangeKey.Change("BANNED", CHANGE));
    }

    @Test
    void malformedValuesDecodeToNull() {
        assertThat(AuthorizationChangeKey.decode(null)).isNull();
        assertThat(AuthorizationChangeKey.decode("ACTIVE")).isNull();
        assertThat(AuthorizationChangeKey.decode("ACTIVE ")).isNull();
        assertThat(AuthorizationChangeKey.decode(" 1763114400700")).isNull();
        assertThat(AuthorizationChangeKey.decode("ACTIVE yesterday")).isNull();
        assertThat(AuthorizationChangeKey.decode("ACTIVE 2026-11-14T10:00:00Z")).isNull();
    }

    @Test
    void anActiveAccountAdmitsTokensFromTheSecondOfTheChangeOn() {
        var change = new AuthorizationChangeKey.Change("ACTIVE", CHANGE);

        assertThat(change.admits(Instant.parse("2026-11-14T10:00:00Z"))).isTrue();
        assertThat(change.admits(Instant.parse("2026-11-14T09:59:59Z"))).isFalse();
    }

    @Test
    void anInactiveAccountAdmitsNoToken() {
        var change = new AuthorizationChangeKey.Change("BANNED", CHANGE);

        assertThat(change.admits(Instant.parse("2026-11-14T10:05:00Z"))).isFalse();
    }
}
