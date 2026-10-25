package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.AuthorizationGrantStatus;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityStatus;
import com.cinx.auth.identity.domain.IdentitySyncStatus;
import com.cinx.auth.identity.domain.RoleGrant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityAccountRepositoryAdapterTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Mock IdentityAccountJpaRepository accounts;
    @Mock IdentityAccountRoleJpaRepository roles;
    IdentityAccountRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdentityAccountRepositoryAdapter(accounts, roles, Mappers.getMapper(IdentityAccountPersistenceMapper.class));
    }

    @Test
    void findByIdLoadsTheAccountTogetherWithItsGrants() {
        when(accounts.findById("user-1")).thenReturn(Optional.of(row("user-1", 3L)));
        when(roles.findAllByUserId("user-1")).thenReturn(List.of(grantRow("user-1", AuthorizationRole.LEARNER, null)));

        IdentityAccount account = adapter.findById("user-1").orElseThrow();

        assertThat(account.getVersion()).isEqualTo(3L);
        assertThat(account.getGrants()).singleElement().satisfies(grant -> assertThat(grant.getRoleCode()).isEqualTo(AuthorizationRole.LEARNER));
    }

    @Test
    void lockByIdTakesTheWriteLockQueryNotThePlainFinder() {
        when(accounts.lockByUserId("user-1")).thenReturn(Optional.of(row("user-1", 3L)));
        when(roles.findAllByUserId("user-1")).thenReturn(List.of());

        assertThat(adapter.lockById("user-1")).isPresent();

        verify(accounts).lockByUserId("user-1");
        verify(accounts, never()).findById(any());
    }

    @Test
    void anUnknownAccountIsAbsent() {
        when(accounts.findById("ghost")).thenReturn(Optional.empty());

        assertThat(adapter.findById("ghost")).isEmpty();
    }

    @Test
    void aNewAccountIsInsertedWithAllItsGrants() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 2L, NOW);
        account.grantRole(AuthorizationRole.LEARNER, "system", "ONBOARD", null, NOW);
        when(accounts.findById("user-1")).thenReturn(Optional.empty());
        when(accounts.save(any(IdentityAccountJpaEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(roles.findAllByUserId("user-1")).thenReturn(List.of());

        adapter.save(account);

        ArgumentCaptor<IdentityAccountJpaEntity> savedAccount = ArgumentCaptor.forClass(IdentityAccountJpaEntity.class);
        verify(accounts).save(savedAccount.capture());
        assertThat(savedAccount.getValue().getUserId()).isEqualTo("user-1");
        assertThat(savedAccount.getValue().getVersion()).isNull();
        ArgumentCaptor<IdentityAccountRoleJpaEntity> savedRole = ArgumentCaptor.forClass(IdentityAccountRoleJpaEntity.class);
        verify(roles).save(savedRole.capture());
        assertThat(savedRole.getValue().getUserId()).isEqualTo("user-1");
        assertThat(savedRole.getValue().getRoleCode()).isEqualTo(AuthorizationRole.LEARNER);
    }

    @Test
    void anExistingAccountIsUpdatedInPlaceSoItsLockVersionStaysTheStoredOne() {
        IdentityAccountJpaEntity stored = row("user-1", 5L);
        when(accounts.findById("user-1")).thenReturn(Optional.of(stored));
        when(accounts.save(stored)).thenReturn(stored);
        when(roles.findAllByUserId("user-1")).thenReturn(List.of());
        IdentityAccount account = IdentityAccount.builder().userId("user-1").version(1L).status(IdentityStatus.BANNED)
                .syncStatus(IdentitySyncStatus.PENDING).authorizationVersion(9L).authorizationPolicyVersion(2L)
                .createdAt(NOW).updatedAt(NOW).build();

        adapter.save(account);

        assertThat(stored.getStatus()).isEqualTo(IdentityStatus.BANNED);
        assertThat(stored.getAuthorizationVersion()).isEqualTo(9L);
        // the account in hand carried version 1, but the row keeps what the database holds
        assertThat(stored.getVersion()).isEqualTo(5L);
    }

    @Test
    void anExistingGrantRowIsUpdatedAndANewOneInserted() {
        IdentityAccountRoleJpaEntity existing = grantRow("user-1", AuthorizationRole.LEARNER, null);
        when(accounts.findById("user-1")).thenReturn(Optional.of(row("user-1", 1L)));
        when(accounts.save(any(IdentityAccountJpaEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(roles.findAllByUserId("user-1")).thenReturn(List.of(existing));
        IdentityAccount account = IdentityAccount.builder().userId("user-1").status(IdentityStatus.ACTIVE).syncStatus(IdentitySyncStatus.SYNCED)
                .authorizationVersion(1L).authorizationPolicyVersion(1L).createdAt(NOW).updatedAt(NOW)
                .grants(new ArrayList<>(List.of(
                        RoleGrant.builder().roleCode(AuthorizationRole.LEARNER).grantedBy("system").grantedAt(NOW)
                                .status(AuthorizationGrantStatus.REVOKED).revokedAt(NOW).source("ONBOARD").build(),
                        RoleGrant.builder().roleCode(AuthorizationRole.SUPPORT_AGENT).grantedBy("admin").grantedAt(NOW)
                                .status(AuthorizationGrantStatus.ACTIVE).source("IDENTITY_ADMIN").build()))).build();

        adapter.save(account);

        assertThat(existing.getStatus()).isEqualTo(AuthorizationGrantStatus.REVOKED);
        verify(roles).save(existing);
        ArgumentCaptor<IdentityAccountRoleJpaEntity> saved = ArgumentCaptor.forClass(IdentityAccountRoleJpaEntity.class);
        verify(roles, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(IdentityAccountRoleJpaEntity::getRoleCode)
                .containsExactlyInAnyOrder(AuthorizationRole.LEARNER, AuthorizationRole.SUPPORT_AGENT);
    }

    @Test
    void aStoredGrantThatIsNotInTheAggregateIsLeftAlone() {
        when(accounts.findById("user-1")).thenReturn(Optional.of(row("user-1", 1L)));
        when(accounts.save(any(IdentityAccountJpaEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(roles.findAllByUserId("user-1")).thenReturn(List.of(grantRow("user-1", AuthorizationRole.LEARNER, null)));
        IdentityAccount account = IdentityAccount.builder().userId("user-1").status(IdentityStatus.ACTIVE).syncStatus(IdentitySyncStatus.SYNCED)
                .authorizationVersion(1L).authorizationPolicyVersion(1L).createdAt(NOW).updatedAt(NOW).build();

        adapter.save(account);

        verify(roles, never()).save(any());
        verify(roles, never()).delete(any());
        verify(roles, never()).deleteAll(any());
    }

    @Test
    void accountsWithDueGrantsAreLockedOncePerUserInTheOrderTheGrantsCame() {
        when(roles.lockDue(NOW)).thenReturn(List.of(
                grantRow("user-b", AuthorizationRole.SUPPORT_AGENT, NOW.minusHours(1)),
                grantRow("user-a", AuthorizationRole.SUPPORT_AGENT, NOW.minusHours(1)),
                grantRow("user-b", AuthorizationRole.REPORT_ANALYST, NOW.minusHours(2))));
        when(accounts.lockByUserId("user-a")).thenReturn(Optional.of(row("user-a", 1L)));
        when(accounts.lockByUserId("user-b")).thenReturn(Optional.of(row("user-b", 1L)));
        when(roles.findAllByUserId("user-a")).thenReturn(List.of());
        when(roles.findAllByUserId("user-b")).thenReturn(List.of());

        List<IdentityAccount> locked = adapter.lockAccountsWithDueGrants(NOW);

        assertThat(locked).extracting(IdentityAccount::getUserId).containsExactly("user-b", "user-a");
    }

    private static IdentityAccountJpaEntity row(String userId, long version) {
        return IdentityAccountJpaEntity.builder().userId(userId).status(IdentityStatus.ACTIVE).syncStatus(IdentitySyncStatus.SYNCED)
                .authorizationVersion(1L).authorizationPolicyVersion(1L).createdAt(NOW).updatedAt(NOW).version(version).build();
    }

    private static IdentityAccountRoleJpaEntity grantRow(String userId, AuthorizationRole role, LocalDateTime expiresAt) {
        return IdentityAccountRoleJpaEntity.builder().userId(userId).roleCode(role).grantedBy("system").grantedAt(NOW)
                .expiresAt(expiresAt).status(AuthorizationGrantStatus.ACTIVE).source("TEST").build();
    }
}
