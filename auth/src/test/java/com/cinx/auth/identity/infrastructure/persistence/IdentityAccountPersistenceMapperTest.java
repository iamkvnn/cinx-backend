package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityAccountPersistenceMapperTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);
    private final IdentityAccountPersistenceMapper mapper = Mappers.getMapper(IdentityAccountPersistenceMapper.class);

    @Test
    void anAccountSurvivesTheTripThroughTheEntityWithEveryField() {
        IdentityAccount original = IdentityAccount.builder().userId("user-1").status(IdentityStatus.BANNED)
                .changedAt(NOW.minusHours(1)).banReason("SPAM").banDetails("links").banExpiresAt(NOW.plusDays(7))
                .createdAt(NOW.minusDays(30)).updatedAt(NOW).version(4L).build();

        IdentityAccount roundTrip = mapper.toDomain(mapper.toEntity(original));

        assertThat(roundTrip).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void updatingARowCopiesWhatTheDomainChangesButNeverItsKeyOrLockVersion() {
        IdentityAccountJpaEntity row = IdentityAccountJpaEntity.builder().userId("user-1").version(5L)
                .status(IdentityStatus.ACTIVE).createdAt(NOW.minusDays(1)).updatedAt(NOW.minusDays(1)).build();
        IdentityAccount changed = IdentityAccount.builder().userId("other").version(99L).status(IdentityStatus.BANNED)
                .changedAt(NOW).banReason("SPAM").banDetails("d").banExpiresAt(NOW.plusDays(1))
                .createdAt(NOW.minusDays(1)).updatedAt(NOW).build();

        mapper.update(row, changed);

        assertThat(row.getUserId()).isEqualTo("user-1");
        assertThat(row.getVersion()).isEqualTo(5L);
        assertThat(row.getStatus()).isEqualTo(IdentityStatus.BANNED);
        assertThat(row.getChangedAt()).isEqualTo(NOW);
        assertThat(row.getBanReason()).isEqualTo("SPAM");
        assertThat(row.getUpdatedAt()).isEqualTo(NOW);
    }
}
