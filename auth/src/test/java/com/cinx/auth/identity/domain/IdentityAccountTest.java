package com.cinx.auth.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityAccountTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Test
    void onboardingStartsActiveAtVersionOneWaitingForItsFirstSync() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 4L, NOW);

        assertThat(account.getUserId()).isEqualTo("user-1");
        assertThat(account.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(account.getSyncStatus()).isEqualTo(IdentitySyncStatus.PENDING);
        assertThat(account.getAuthorizationVersion()).isEqualTo(1L);
        assertThat(account.getAuthorizationPolicyVersion()).isEqualTo(4L);
        assertThat(account.getCreatedAt()).isEqualTo(NOW);
        assertThat(account.getUpdatedAt()).isEqualTo(NOW);
        assertThat(account.getVersion()).isNull();
        assertThat(account.getGrants()).isEmpty();
    }

    @Test
    void grantingANewRoleChangesTheAccountAndRecordsWhoGaveIt() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 1L, NOW);

        boolean changed = account.grantRole(AuthorizationRole.SUPPORT_AGENT, "admin", "IDENTITY_ADMIN", null, NOW);

        assertThat(changed).isTrue();
        assertThat(account.grantOf(AuthorizationRole.SUPPORT_AGENT)).get().satisfies(grant -> {
            assertThat(grant.getGrantedBy()).isEqualTo("admin");
            assertThat(grant.getSource()).isEqualTo("IDENTITY_ADMIN");
            assertThat(grant.getGrantedAt()).isEqualTo(NOW);
            assertThat(grant.getStatus()).isEqualTo(AuthorizationGrantStatus.ACTIVE);
            assertThat(grant.getRevokedAt()).isNull();
        });
    }

    @Test
    void grantingARoleThatIsAlreadyActiveWithTheSameEndChangesNothing() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 1L, NOW);
        account.grantRole(AuthorizationRole.SUPPORT_AGENT, "admin", "IDENTITY_ADMIN", NOW.plusDays(5), NOW);

        boolean changed = account.grantRole(AuthorizationRole.SUPPORT_AGENT, "someone-else", "OTHER", NOW.plusDays(5), NOW.plusHours(1));

        assertThat(changed).isFalse();
        // the first grant is untouched: who gave it and when stay as they were
        assertThat(account.grantOf(AuthorizationRole.SUPPORT_AGENT)).get().satisfies(grant -> {
            assertThat(grant.getGrantedBy()).isEqualTo("admin");
            assertThat(grant.getGrantedAt()).isEqualTo(NOW);
        });
        assertThat(account.getGrants()).hasSize(1);
    }

    @Test
    void grantingAnActiveRoleWithADifferentEndReplacesTheGrant() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 1L, NOW);
        account.grantRole(AuthorizationRole.SUPPORT_AGENT, "admin", "IDENTITY_ADMIN", NOW.plusDays(5), NOW);

        boolean changed = account.grantRole(AuthorizationRole.SUPPORT_AGENT, "boss", "IDENTITY_ADMIN", NOW.plusDays(9), NOW.plusHours(1));

        assertThat(changed).isTrue();
        assertThat(account.getGrants()).singleElement().satisfies(grant -> {
            assertThat(grant.getGrantedBy()).isEqualTo("boss");
            assertThat(grant.getExpiresAt()).isEqualTo(NOW.plusDays(9));
        });
    }

    @Test
    void grantingARoleThatWasRevokedGivesItBackWithAFreshHistory() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 1L, NOW);
        account.grantRole(AuthorizationRole.INSTRUCTOR, "workflow", "INSTRUCTOR_VERIFIED", null, NOW);
        account.revokeRole(AuthorizationRole.INSTRUCTOR, NOW.plusDays(1));

        boolean changed = account.grantRole(AuthorizationRole.INSTRUCTOR, "workflow", "INSTRUCTOR_VERIFIED", null, NOW.plusDays(2));

        assertThat(changed).isTrue();
        assertThat(account.grantOf(AuthorizationRole.INSTRUCTOR)).get().satisfies(grant -> {
            assertThat(grant.getStatus()).isEqualTo(AuthorizationGrantStatus.ACTIVE);
            assertThat(grant.getRevokedAt()).isNull();
            assertThat(grant.getGrantedAt()).isEqualTo(NOW.plusDays(2));
        });
    }

    @Test
    void revokingOnlyTouchesAnActiveGrant() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 1L, NOW);
        account.grantRole(AuthorizationRole.INSTRUCTOR, "workflow", "INSTRUCTOR_VERIFIED", null, NOW);

        assertThat(account.revokeRole(AuthorizationRole.INSTRUCTOR, NOW.plusDays(1))).isTrue();
        assertThat(account.grantOf(AuthorizationRole.INSTRUCTOR)).get().satisfies(grant -> {
            assertThat(grant.getStatus()).isEqualTo(AuthorizationGrantStatus.REVOKED);
            assertThat(grant.getRevokedAt()).isEqualTo(NOW.plusDays(1));
        });
        // already revoked, and a role never held: nothing to revoke
        assertThat(account.revokeRole(AuthorizationRole.INSTRUCTOR, NOW.plusDays(2))).isFalse();
        assertThat(account.revokeRole(AuthorizationRole.FINANCE_OPERATOR, NOW.plusDays(2))).isFalse();
        assertThat(account.grantOf(AuthorizationRole.INSTRUCTOR).orElseThrow().getRevokedAt()).isEqualTo(NOW.plusDays(1));
    }

    @Test
    void aGrantIsEffectiveUntilTheMomentItEnds() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 1L, NOW);
        account.grantRole(AuthorizationRole.LEARNER, "system", "ONBOARD", null, NOW);
        account.grantRole(AuthorizationRole.SUPPORT_AGENT, "admin", "IDENTITY_ADMIN", NOW.plusHours(1), NOW);

        // an hour before the end, exactly at the end, and after it
        assertThat(account.effectiveRoles(NOW)).containsExactlyInAnyOrder(AuthorizationRole.LEARNER, AuthorizationRole.SUPPORT_AGENT);
        assertThat(account.effectiveRoles(NOW.plusHours(1))).containsExactly(AuthorizationRole.LEARNER);
        assertThat(account.effectiveRoles(NOW.plusHours(2))).containsExactly(AuthorizationRole.LEARNER);
    }

    @Test
    void aRevokedGrantIsNeverEffective() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 1L, NOW);
        account.grantRole(AuthorizationRole.INSTRUCTOR, "workflow", "INSTRUCTOR_VERIFIED", null, NOW);
        account.revokeRole(AuthorizationRole.INSTRUCTOR, NOW);

        assertThat(account.effectiveGrants(NOW)).isEmpty();
    }

    @Test
    void expiringMarksOnlyActiveGrantsPastTheirEndAndReturnsThem() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 1L, NOW);
        account.grantRole(AuthorizationRole.LEARNER, "system", "ONBOARD", null, NOW);
        account.grantRole(AuthorizationRole.SUPPORT_AGENT, "admin", "IDENTITY_ADMIN", NOW.plusHours(1), NOW);
        account.grantRole(AuthorizationRole.REPORT_ANALYST, "admin", "IDENTITY_ADMIN", NOW.plusDays(2), NOW);
        account.grantRole(AuthorizationRole.FINANCE_OPERATOR, "admin", "IDENTITY_ADMIN", NOW.plusMinutes(30), NOW);
        account.revokeRole(AuthorizationRole.FINANCE_OPERATOR, NOW.plusMinutes(10));

        List<RoleGrant> expired = account.expireDueGrants(NOW.plusHours(1));

        assertThat(expired).extracting(RoleGrant::getRoleCode).containsExactly(AuthorizationRole.SUPPORT_AGENT);
        assertThat(account.grantOf(AuthorizationRole.SUPPORT_AGENT).orElseThrow().getStatus()).isEqualTo(AuthorizationGrantStatus.EXPIRED);
        assertThat(account.grantOf(AuthorizationRole.SUPPORT_AGENT).orElseThrow().getRevokedAt()).isEqualTo(NOW.plusHours(1));
        // open-ended, still in the future, and already revoked grants keep their state
        assertThat(account.grantOf(AuthorizationRole.LEARNER).orElseThrow().getStatus()).isEqualTo(AuthorizationGrantStatus.ACTIVE);
        assertThat(account.grantOf(AuthorizationRole.REPORT_ANALYST).orElseThrow().getStatus()).isEqualTo(AuthorizationGrantStatus.ACTIVE);
        assertThat(account.grantOf(AuthorizationRole.FINANCE_OPERATOR).orElseThrow().getStatus()).isEqualTo(AuthorizationGrantStatus.REVOKED);
    }

    @Test
    void incrementingTheAuthorizationVersionAddsOne() {
        IdentityAccount account = IdentityAccount.onboard("user-1", 1L, NOW);

        account.incrementAuthorizationVersion();
        account.incrementAuthorizationVersion();

        assertThat(account.getAuthorizationVersion()).isEqualTo(3L);
    }
}
