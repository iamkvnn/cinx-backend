package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.application.query.AccountRoles;
import com.cinx.auth.identity.domain.AuthorizationGrantStatus;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAuthorizationQueriesAdapterTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Mock IdentityAccountJpaRepository accounts;
    @Mock IdentityAccountRoleJpaRepository roles;

    @Test
    void everyActiveAccountGetsItsEffectiveRolesAndOnesWithNoneGetAnEmptySet() {
        when(accounts.findAllByStatus(IdentityStatus.ACTIVE)).thenReturn(List.of(account("a"), account("b"), account("c")));
        when(roles.findAllEffective(NOW)).thenReturn(List.of(
                role("a", AuthorizationRole.LEARNER), role("a", AuthorizationRole.SUPPORT_AGENT), role("b", AuthorizationRole.LEARNER),
                // a grant row of an account that is not active is ignored
                role("banned", AuthorizationRole.PLATFORM_ADMIN)));

        List<AccountRoles> result = new AccountAuthorizationQueriesAdapter(accounts, roles).findActiveAccountRoles(NOW);

        assertThat(result).containsExactly(
                new AccountRoles("a", Set.of(AuthorizationRole.LEARNER, AuthorizationRole.SUPPORT_AGENT)),
                new AccountRoles("b", Set.of(AuthorizationRole.LEARNER)),
                new AccountRoles("c", Set.of()));
    }

    private static IdentityAccountJpaEntity account(String userId) {
        return IdentityAccountJpaEntity.builder().userId(userId).status(IdentityStatus.ACTIVE).build();
    }

    private static IdentityAccountRoleJpaEntity role(String userId, AuthorizationRole role) {
        return IdentityAccountRoleJpaEntity.builder().userId(userId).roleCode(role).status(AuthorizationGrantStatus.ACTIVE).build();
    }
}
