package com.cinx.auth.identity.application;

import com.cinx.auth.identity.application.command.BanUserCommand;
import com.cinx.auth.identity.application.command.LinkIdentityCommand;
import com.cinx.auth.identity.application.command.UpdateStaffRolesCommand;
import com.cinx.auth.identity.application.query.AccountAuthorizationQueries;
import com.cinx.auth.identity.application.query.AccountRoles;
import com.cinx.auth.identity.domain.AuthorizationGrantStatus;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.BanReasonType;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityAccountRepository;
import com.cinx.auth.identity.domain.IdentityStatus;
import com.cinx.auth.identity.domain.IdentitySyncStatus;
import com.cinx.auth.identity.domain.KeycloakSyncJob;
import com.cinx.auth.identity.domain.KeycloakSyncJobRepository;
import com.cinx.auth.identity.domain.RoleGrant;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 0, 0);

    @Mock IdentityAccountRepository accounts;
    @Mock KeycloakSyncJobRepository syncJobs;
    @Mock AuthorizationAuditLog audits;
    @Mock IdentityEvents events;
    @Mock AuthorizationCatalog catalog;
    @Mock AccountAuthorizationQueries queries;
    IdentityService service;

    @BeforeEach
    void setUp() {
        service = new IdentityService(accounts, syncJobs, audits, events, catalog, queries,
                Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void onboardingACreatesANewActiveAccountThatHoldsLearnerAndQueuesASync() {
        when(catalog.policyVersion()).thenReturn(3L);
        when(accounts.lockById("user-1")).thenReturn(Optional.empty());

        var view = service.link("user-1", new LinkIdentityCommand("kc-1", "u@example.com"));

        ArgumentCaptor<IdentityAccount> saved = ArgumentCaptor.forClass(IdentityAccount.class);
        verify(accounts).save(saved.capture());
        IdentityAccount account = saved.getValue();
        assertThat(account.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(account.getSyncStatus()).isEqualTo(IdentitySyncStatus.PENDING);
        assertThat(account.getAuthorizationVersion()).isEqualTo(1L);
        assertThat(account.getAuthorizationPolicyVersion()).isEqualTo(3L);
        assertThat(account.getKeycloakSubject()).isEqualTo("kc-1");
        assertThat(account.getEmail()).isEqualTo("u@example.com");
        assertThat(account.getGrants()).singleElement().satisfies(grant -> {
            assertThat(grant.getRoleCode()).isEqualTo(AuthorizationRole.LEARNER);
            assertThat(grant.getSource()).isEqualTo("USER_PROFILE_ONBOARDED");
            assertThat(grant.getGrantedAt()).isEqualTo(NOW);
        });
        assertThat(view.userId()).isEqualTo("user-1");

        ArgumentCaptor<KeycloakSyncJob> job = ArgumentCaptor.forClass(KeycloakSyncJob.class);
        verify(syncJobs).save(job.capture());
        assertThat(job.getValue().getDesiredAuthorizationVersion()).isEqualTo(1L);
        assertThat(job.getValue().getStatus()).isEqualTo(KeycloakSyncJob.Status.PENDING);
        verify(audits).append(org.mockito.ArgumentMatchers.argThat(entry -> entry.action().equals("ACCOUNT_ONBOARDED")
                && entry.targetUserId().equals("user-1") && entry.afterJson().contains("\"authorizationVersion\":1")));
        verify(events).authorizationChanged(eq(account), eq(Set.of(AuthorizationRole.LEARNER)));
    }

    @Test
    void onboardingAnExistingAccountThatAlreadyHoldsLearnerKeepsItsVersion() {
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null));
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        service.link("user-1", new LinkIdentityCommand("kc-new", "new@example.com"));

        assertThat(account.getAuthorizationVersion()).isEqualTo(7L);
        assertThat(account.getKeycloakSubject()).isEqualTo("kc-new");
        assertThat(account.getEmail()).isEqualTo("new@example.com");
        verify(audits).append(org.mockito.ArgumentMatchers.argThat(entry -> entry.action().equals("ACCOUNT_RECONCILED")));
    }

    @Test
    void onboardingAnExistingAccountThatLostLearnerRestoresItAndBumpsTheVersion() {
        IdentityAccount account = account(7L);
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        service.link("user-1", new LinkIdentityCommand("kc-1", null));

        assertThat(account.getAuthorizationVersion()).isEqualTo(8L);
        assertThat(account.effectiveRoles(NOW)).containsExactly(AuthorizationRole.LEARNER);
    }

    @Test
    void suspensionIncrementsVersionRecordsTheBanAndQueuesASync() {
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null));
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        service.ban("user-1", new BanUserCommand(BanReasonType.SPAM, "reason", 7));

        assertThat(account.getStatus()).isEqualTo(IdentityStatus.BANNED);
        assertThat(account.getAuthorizationVersion()).isEqualTo(8L);
        assertThat(account.getBanReason()).isEqualTo("SPAM");
        assertThat(account.getBanDetails()).isEqualTo("reason");
        // 7 days after the fixed clock
        assertThat(account.getBanExpiresAt()).isEqualTo(NOW.plusDays(7));
        assertThat(account.getSyncStatus()).isEqualTo(IdentitySyncStatus.PENDING);
        verify(accounts).save(account);
        verify(syncJobs).save(any());
        verify(events).authorizationChanged(eq(account), anyCollectionOfRoles());
    }

    @Test
    void aSuspensionWithoutADurationHasNoEndDate() {
        IdentityAccount account = account(7L);
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        service.ban("user-1", new BanUserCommand(BanReasonType.SPAM, "reason", null));

        assertThat(account.getBanExpiresAt()).isNull();
    }

    @Test
    void suspendingAnAlreadySuspendedAccountDoesNotBumpTheVersionAgain() {
        IdentityAccount account = account(7L);
        account.setStatus(IdentityStatus.BANNED);
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        service.ban("user-1", new BanUserCommand(BanReasonType.SPAM, "again", 1));

        assertThat(account.getAuthorizationVersion()).isEqualTo(7L);
    }

    @Test
    void reactivationClearsTheBanAndBumpsTheVersionOnlyWhenTheStatusChanges() {
        IdentityAccount banned = account(7L);
        banned.setStatus(IdentityStatus.BANNED);
        banned.setBanReason("SPAM");
        banned.setBanDetails("reason");
        banned.setBanExpiresAt(NOW.plusDays(3));
        IdentityAccount active = account(7L);
        when(accounts.lockById("banned")).thenReturn(Optional.of(banned));
        when(accounts.lockById("active")).thenReturn(Optional.of(active));

        service.unban("banned");
        service.unban("active");

        assertThat(banned.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(banned.getBanReason()).isNull();
        assertThat(banned.getBanDetails()).isNull();
        assertThat(banned.getBanExpiresAt()).isNull();
        assertThat(banned.getAuthorizationVersion()).isEqualTo(8L);
        assertThat(active.getAuthorizationVersion()).isEqualTo(7L);
    }

    @Test
    void instructorGrantIsAdditiveAndIdempotent() {
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null));
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        service.promoteInstructor("user-1");

        assertThat(account.getAuthorizationVersion()).isEqualTo(8L);
        assertThat(account.effectiveRoles(NOW)).containsExactlyInAnyOrder(AuthorizationRole.LEARNER, AuthorizationRole.INSTRUCTOR);
        verify(events).authorizationChanged(eq(account), eq(Set.of(AuthorizationRole.LEARNER, AuthorizationRole.INSTRUCTOR)));
        verify(audits).append(org.mockito.ArgumentMatchers.argThat(entry ->
                entry.action().equals("ROLE_GRANTED") && "INSTRUCTOR".equals(entry.roleCode())));

        // a second promotion changes nothing: no version bump, no further save
        service.promoteInstructor("user-1");

        assertThat(account.getAuthorizationVersion()).isEqualTo(8L);
        verify(accounts, org.mockito.Mockito.times(1)).save(account);
    }

    @Test
    void terminatingInstructorRevokesOnlyTheInstructorGrant() {
        RoleGrant instructor = grant(AuthorizationRole.INSTRUCTOR, null);
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null), instructor);
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        service.terminateInstructor("user-1");

        assertThat(instructor.getStatus()).isEqualTo(AuthorizationGrantStatus.REVOKED);
        assertThat(instructor.getRevokedAt()).isEqualTo(NOW);
        assertThat(account.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(account.getAuthorizationVersion()).isEqualTo(8L);
        assertThat(account.effectiveRoles(NOW)).containsExactly(AuthorizationRole.LEARNER);
        verify(accounts).save(account);
    }

    @Test
    void terminatingSomeoneWhoIsNotAnInstructorChangesAndSavesNothing() {
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null));
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        service.terminateInstructor("user-1");

        assertThat(account.getAuthorizationVersion()).isEqualTo(7L);
        verify(accounts, never()).save(any());
        verify(events, never()).authorizationChanged(any(), anyCollectionOfRoles());
    }

    @Test
    void anExistingSyncJobForTheSameVersionIsNotQueuedTwice() {
        IdentityAccount account = account(7L);
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));
        when(syncJobs.existsByUserIdAndDesiredAuthorizationVersion("user-1", 8L)).thenReturn(true);

        service.ban("user-1", new BanUserCommand(BanReasonType.SPAM, "reason", null));

        verify(syncJobs, never()).save(any());
    }

    @Test
    void staffRolesAreGrantedAndTheUnlistedActiveOnesRevoked() {
        RoleGrant keep = grant(AuthorizationRole.SUPPORT_AGENT, null);
        RoleGrant drop = grant(AuthorizationRole.FINANCE_OPERATOR, null);
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null), keep, drop);
        when(accounts.lockById("target")).thenReturn(Optional.of(account));

        service.updateStaffRoles("admin", "target", new UpdateStaffRolesCommand(
                Set.of(AuthorizationRole.SUPPORT_AGENT, AuthorizationRole.REPORT_ANALYST), null));

        assertThat(account.effectiveRoles(NOW)).containsExactlyInAnyOrder(
                AuthorizationRole.LEARNER, AuthorizationRole.SUPPORT_AGENT, AuthorizationRole.REPORT_ANALYST);
        assertThat(drop.getStatus()).isEqualTo(AuthorizationGrantStatus.REVOKED);
        assertThat(account.getAuthorizationVersion()).isEqualTo(8L);
        verify(audits).append(org.mockito.ArgumentMatchers.argThat(entry -> entry.action().equals("STAFF_ROLE_SET_UPDATED")));
    }

    @Test
    void staffRolesThatAreAlreadyAsRequestedChangeNothing() {
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null), grant(AuthorizationRole.SUPPORT_AGENT, null));
        when(accounts.lockById("target")).thenReturn(Optional.of(account));

        service.updateStaffRoles("admin", "target", new UpdateStaffRolesCommand(Set.of(AuthorizationRole.SUPPORT_AGENT), null));

        assertThat(account.getAuthorizationVersion()).isEqualTo(7L);
        verify(accounts, never()).save(any());
    }

    @Test
    void anAdminCannotChangeTheirOwnRoles() {
        assertThatThrownBy(() -> service.updateStaffRoles("admin", "admin",
                new UpdateStaffRolesCommand(Set.of(AuthorizationRole.SUPPORT_AGENT), null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Self role assignment");
        verify(accounts, never()).lockById(any());
    }

    @Test
    void rolesThatNeedADedicatedWorkflowCannotBeSetThisWay() {
        for (AuthorizationRole role : List.of(AuthorizationRole.LEARNER, AuthorizationRole.INSTRUCTOR,
                AuthorizationRole.PLATFORM_ADMIN, AuthorizationRole.BREAK_GLASS_ADMIN)) {
            assertThatThrownBy(() -> service.updateStaffRoles("admin", "target", new UpdateStaffRolesCommand(Set.of(role), null)))
                    .as(role.name()).isInstanceOf(BadRequestException.class).hasMessageContaining("dedicated approval workflow");
        }
    }

    @Test
    void aRoleExpiryInThePastOrNowIsRefused() {
        assertThatThrownBy(() -> service.updateStaffRoles("admin", "target", new UpdateStaffRolesCommand(
                Set.of(AuthorizationRole.SUPPORT_AGENT), Instant.parse("2026-08-01T00:00:00Z"))))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("expiry must be in the future");
    }

    @Test
    void aFutureRoleExpiryIsStoredOnTheGrant() {
        IdentityAccount account = account(7L);
        when(accounts.lockById("target")).thenReturn(Optional.of(account));

        service.updateStaffRoles("admin", "target", new UpdateStaffRolesCommand(
                Set.of(AuthorizationRole.SUPPORT_AGENT), Instant.parse("2026-09-01T00:00:00Z")));

        assertThat(account.grantOf(AuthorizationRole.SUPPORT_AGENT)).get().satisfies(grant -> {
            assertThat(grant.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
            assertThat(grant.getGrantedBy()).isEqualTo("admin");
            assertThat(grant.getSource()).isEqualTo("IDENTITY_ADMIN");
        });
    }

    @Test
    void expiredGrantsAreMarkedAuditedAndBumpTheVersion() {
        RoleGrant due = grant(AuthorizationRole.SUPPORT_AGENT, NOW.minusHours(1));
        RoleGrant later = grant(AuthorizationRole.REPORT_ANALYST, NOW.plusDays(1));
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null), due, later);
        when(accounts.lockAccountsWithDueGrants(NOW)).thenReturn(List.of(account));

        service.expireDueGrants();

        assertThat(due.getStatus()).isEqualTo(AuthorizationGrantStatus.EXPIRED);
        assertThat(due.getRevokedAt()).isEqualTo(NOW);
        assertThat(later.getStatus()).isEqualTo(AuthorizationGrantStatus.ACTIVE);
        assertThat(account.getAuthorizationVersion()).isEqualTo(8L);
        verify(audits).append(org.mockito.ArgumentMatchers.argThat(entry -> entry.action().equals("ROLE_EXPIRED")
                && "SUPPORT_AGENT".equals(entry.roleCode()) && entry.actorId().equals("authorization-expiry-worker")));
        verify(audits).append(org.mockito.ArgumentMatchers.argThat(entry -> entry.action().equals("AUTHORIZATION_EXPIRY_APPLIED")));
        verify(accounts).save(account);
    }

    @Test
    void anAccountWhoseDueGrantsWereAlreadyHandledIsLeftAlone() {
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null));
        when(accounts.lockAccountsWithDueGrants(NOW)).thenReturn(List.of(account));

        service.expireDueGrants();

        assertThat(account.getAuthorizationVersion()).isEqualTo(7L);
        verify(accounts, never()).save(any());
        verify(audits, never()).append(any());
    }

    @Test
    void authorizationStateListsOnlyRolesInForceSortedByName() {
        IdentityAccount account = account(7L, grant(AuthorizationRole.SUPPORT_AGENT, null), grant(AuthorizationRole.LEARNER, null),
                grant(AuthorizationRole.REPORT_ANALYST, NOW.minusMinutes(1)));
        when(accounts.findById("user-1")).thenReturn(Optional.of(account));

        var state = service.authorizationState("user-1");

        // the analyst grant ended a minute ago, so it no longer counts
        assertThat(state.roles()).containsExactly("LEARNER", "SUPPORT_AGENT");
        assertThat(state.authorizationVersion()).isEqualTo(7L);
        assertThat(state.authorizationPolicyVersion()).isEqualTo(1L);
        assertThat(state.evaluatedAt()).isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
    }

    @Test
    void findAccountOfAnUnknownUserIsNotFound() {
        when(accounts.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAccount("ghost")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void lockingAnUnknownAccountIsNotFound() {
        when(accounts.lockById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ban("ghost", new BanUserCommand(BanReasonType.SPAM, "reason", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void recipientLookupKeepsOnlyActiveAccountsWhoseRolesCarryThePermission() {
        when(queries.findActiveAccountRoles(NOW)).thenReturn(List.of(
                new AccountRoles("a", Set.of(AuthorizationRole.NOTIFICATION_OPERATOR)),
                new AccountRoles("b", Set.of(AuthorizationRole.LEARNER)),
                new AccountRoles("c", Set.of())));
        when(catalog.effectivePermissions(Set.of(AuthorizationRole.NOTIFICATION_OPERATOR))).thenReturn(Set.of("notification.broadcast.send.any"));
        when(catalog.effectivePermissions(Set.of(AuthorizationRole.LEARNER))).thenReturn(Set.of("learning.course.read"));
        when(catalog.effectivePermissions(Set.of())).thenReturn(Set.of());

        assertThat(service.findUserIdsByPermission("notification.broadcast.send.any")).containsExactly("a");
    }

    @Test
    void recipientLookupRefusesPermissionsOutsideTheAllowlist() {
        assertThatThrownBy(() -> service.findUserIdsByPermission("auth.account.suspend.any"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(queries, never()).findActiveAccountRoles(any());
    }

    private static java.util.Set<AuthorizationRole> anyCollectionOfRoles() {
        return org.mockito.ArgumentMatchers.anySet();
    }

    private static IdentityAccount account(long version, RoleGrant... grants) {
        return IdentityAccount.builder().userId("user-1").keycloakSubject("kc-1")
                .status(IdentityStatus.ACTIVE).syncStatus(IdentitySyncStatus.SYNCED)
                .authorizationVersion(version).authorizationPolicyVersion(1L)
                .createdAt(NOW).updatedAt(NOW).grants(new ArrayList<>(List.of(grants))).build();
    }

    private static RoleGrant grant(AuthorizationRole role, LocalDateTime expiresAt) {
        return RoleGrant.builder().roleCode(role).grantedBy("test").grantedAt(NOW.minusDays(1)).expiresAt(expiresAt)
                .status(AuthorizationGrantStatus.ACTIVE).source("TEST").build();
    }
}
