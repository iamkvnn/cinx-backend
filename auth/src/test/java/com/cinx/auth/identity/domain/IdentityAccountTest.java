package com.cinx.auth.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityAccountTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Test
    void aNewAccountIsActiveAndNeverChanged() {
        IdentityAccount account = IdentityAccount.create("user-1", NOW);

        assertThat(account.getUserId()).isEqualTo("user-1");
        assertThat(account.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(account.getChangedAt()).isNull();
        assertThat(account.getCreatedAt()).isEqualTo(NOW);
        assertThat(account.getVersion()).isNull();
    }

    @Test
    void markingAChangeStampsBothTheChangeAndTheUpdate() {
        IdentityAccount account = IdentityAccount.create("user-1", NOW.minusDays(1));

        account.markChanged(NOW);

        assertThat(account.getChangedAt()).isEqualTo(NOW);
        assertThat(account.getUpdatedAt()).isEqualTo(NOW);
    }
}
