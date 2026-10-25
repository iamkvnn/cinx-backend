package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.AuthorizationGrantStatus;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityStatus;
import com.cinx.auth.identity.domain.IdentitySyncStatus;
import com.cinx.auth.identity.domain.RoleGrant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityAccountPersistenceMapperTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);
    private final IdentityAccountPersistenceMapper mapper = Mappers.getMapper(IdentityAccountPersistenceMapper.class);

    @Test
    void anAccountSurvivesTheTripThroughTheEntityWithEveryField() {
        IdentityAccount original = IdentityAccount.builder().userId("user-1").keycloakSubject("kc-1").email("u@example.com")
                .status(IdentityStatus.BANNED).syncStatus(IdentitySyncStatus.FAILED)
                .authorizationVersion(9L).authorizationPolicyVersion(3L)
                .banReason("SPAM").banDetails("links").banExpiresAt(NOW.plusDays(7))
                .createdAt(NOW.minusDays(30)).updatedAt(NOW).version(4L).build();

        IdentityAccount roundTrip = mapper.toDomain(mapper.toEntity(original));

        assertThat(roundTrip).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void theLockVersionTravelsBothWays() {
        IdentityAccountJpaEntity entity = mapper.toEntity(IdentityAccount.builder().userId("user-1").version(6L)
                .status(IdentityStatus.ACTIVE).syncStatus(IdentitySyncStatus.SYNCED).authorizationVersion(1L)
                .authorizationPolicyVersion(1L).createdAt(NOW).updatedAt(NOW).build());

        assertThat(entity.getVersion()).isEqualTo(6L);
        assertThat(mapper.toDomain(entity).getVersion()).isEqualTo(6L);
    }

    @Test
    void theAccountComesBackWithoutGrantsUntilTheAdapterAddsThem() {
        IdentityAccount account = mapper.toDomain(IdentityAccountJpaEntity.builder().userId("user-1")
                .status(IdentityStatus.ACTIVE).syncStatus(IdentitySyncStatus.SYNCED).authorizationVersion(1L)
                .authorizationPolicyVersion(1L).createdAt(NOW).updatedAt(NOW).build());

        assertThat(account.getGrants()).isNotNull().isEmpty();
    }

    @Test
    void updatingARowCopiesWhatTheDomainChangesButNeverItsKeyOrLockVersion() {
        IdentityAccountJpaEntity row = IdentityAccountJpaEntity.builder().userId("user-1").version(5L)
                .status(IdentityStatus.ACTIVE).syncStatus(IdentitySyncStatus.SYNCED).authorizationVersion(7L)
                .authorizationPolicyVersion(1L).createdAt(NOW.minusDays(1)).updatedAt(NOW.minusDays(1)).build();
        IdentityAccount changed = IdentityAccount.builder().userId("other").version(99L).keycloakSubject("kc-2").email("n@example.com")
                .status(IdentityStatus.BANNED).syncStatus(IdentitySyncStatus.PENDING).authorizationVersion(8L)
                .authorizationPolicyVersion(2L).banReason("SPAM").banDetails("d").banExpiresAt(NOW.plusDays(1))
                .createdAt(NOW.minusDays(1)).updatedAt(NOW).build();

        mapper.update(row, changed);

        assertThat(row.getUserId()).isEqualTo("user-1");
        assertThat(row.getVersion()).isEqualTo(5L);
        assertThat(row.getStatus()).isEqualTo(IdentityStatus.BANNED);
        assertThat(row.getSyncStatus()).isEqualTo(IdentitySyncStatus.PENDING);
        assertThat(row.getAuthorizationVersion()).isEqualTo(8L);
        assertThat(row.getAuthorizationPolicyVersion()).isEqualTo(2L);
        assertThat(row.getKeycloakSubject()).isEqualTo("kc-2");
        assertThat(row.getBanReason()).isEqualTo("SPAM");
        assertThat(row.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void aGrantSurvivesTheTripAndTakesItsOwnerFromTheAccount() {
        RoleGrant original = RoleGrant.builder().roleCode(AuthorizationRole.SUPPORT_AGENT).grantedBy("admin").grantedAt(NOW)
                .expiresAt(NOW.plusDays(5)).status(AuthorizationGrantStatus.REVOKED).revokedAt(NOW.plusDays(1)).source("IDENTITY_ADMIN").build();

        IdentityAccountRoleJpaEntity row = mapper.toEntity(original, "user-1");

        assertThat(row.getUserId()).isEqualTo("user-1");
        assertThat(row.getRoleCode()).isEqualTo(AuthorizationRole.SUPPORT_AGENT);
        assertThat(mapper.toGrant(row)).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void updatingAGrantRowKeepsItsCompositeKey() {
        IdentityAccountRoleJpaEntity row = IdentityAccountRoleJpaEntity.builder().userId("user-1").roleCode(AuthorizationRole.SUPPORT_AGENT)
                .grantedBy("admin").grantedAt(NOW).status(AuthorizationGrantStatus.ACTIVE).source("A").build();
        RoleGrant changed = RoleGrant.builder().roleCode(AuthorizationRole.FINANCE_OPERATOR).grantedBy("boss").grantedAt(NOW.plusDays(1))
                .status(AuthorizationGrantStatus.EXPIRED).revokedAt(NOW.plusDays(2)).source("B").build();

        mapper.update(row, changed);

        assertThat(row.getUserId()).isEqualTo("user-1");
        assertThat(row.getRoleCode()).isEqualTo(AuthorizationRole.SUPPORT_AGENT);
        assertThat(row.getGrantedBy()).isEqualTo("boss");
        assertThat(row.getStatus()).isEqualTo(AuthorizationGrantStatus.EXPIRED);
        assertThat(row.getRevokedAt()).isEqualTo(NOW.plusDays(2));
        assertThat(row.getSource()).isEqualTo("B");
    }
}
