package com.cinx.auth.identity.application;

import com.cinx.auth.identity.domain.AuthorizationGrantStatus;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityAccountRepository;
import com.cinx.auth.identity.domain.IdentityStatus;
import com.cinx.auth.identity.domain.IdentitySyncStatus;
import com.cinx.auth.identity.domain.KeycloakSyncJob;
import com.cinx.auth.identity.domain.KeycloakSyncJobRepository;
import com.cinx.auth.identity.domain.RoleGrant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityReconcilerTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 0, 0);

    @Mock IdentityAccountRepository accounts;
    @Mock KeycloakSyncJobRepository jobs;
    @Mock KeycloakAdminClient keycloak;
    IdentityReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new IdentityReconciler(accounts, jobs, keycloak, Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void aDueJobPushesTheEffectiveRolesAndTheEarliestEndToKeycloakThenRevokesSessions() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "user-1", 7L, NOW.minusMinutes(1));
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null),
                grant(AuthorizationRole.SUPPORT_AGENT, NOW.plusDays(9)), grant(AuthorizationRole.REPORT_ANALYST, NOW.plusDays(3)));
        when(jobs.lockDue(NOW)).thenReturn(List.of(job));
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        reconciler.reconcile();

        InOrder order = inOrder(keycloak);
        order.verify(keycloak).synchronizeAuthorization("kc-1", "user-1", true,
                Set.of(AuthorizationRole.LEARNER, AuthorizationRole.SUPPORT_AGENT, AuthorizationRole.REPORT_ANALYST),
                7L, 2L, NOW.plusDays(3).toInstant(ZoneOffset.UTC));
        order.verify(keycloak).revokeSessions("kc-1");
        assertThat(job.getStatus()).isEqualTo(KeycloakSyncJob.Status.SUCCEEDED);
        assertThat(account.getSyncStatus()).isEqualTo(IdentitySyncStatus.SYNCED);
        assertThat(account.getUpdatedAt()).isEqualTo(NOW);
        // the changes are written back explicitly: nothing relies on dirty checking
        verify(accounts).save(account);
        verify(jobs).save(job);
    }

    @Test
    void aBannedAccountIsPushedAsDisabled() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "user-1", 7L, NOW);
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null));
        account.setStatus(IdentityStatus.BANNED);
        when(jobs.lockDue(NOW)).thenReturn(List.of(job));
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        reconciler.reconcile();

        verify(keycloak).synchronizeAuthorization(eq("kc-1"), eq("user-1"), eq(false), anySet(), eq(7L), eq(2L), isNull());
    }

    @Test
    void anAccountWithoutAKeycloakSubjectIsPushedUnderItsUserId() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "user-1", 7L, NOW);
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null));
        account.setKeycloakSubject(null);
        when(jobs.lockDue(NOW)).thenReturn(List.of(job));
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));

        reconciler.reconcile();

        verify(keycloak).revokeSessions("user-1");
    }

    @Test
    void aFailureSchedulesARetryAndMarksTheAccountFailed() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "user-1", 7L, NOW);
        IdentityAccount account = account(7L, grant(AuthorizationRole.LEARNER, null));
        when(jobs.lockDue(NOW)).thenReturn(List.of(job));
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account));
        doThrow(new KeycloakSyncException("down", null)).when(keycloak)
                .synchronizeAuthorization(anyString(), anyString(), anyBoolean(), anySet(), anyLong(), anyLong(), any());

        reconciler.reconcile();

        assertThat(job.getStatus()).isEqualTo(KeycloakSyncJob.Status.RETRYABLE_FAILED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(job.getLastErrorCode()).isEqualTo("KeycloakSyncException");
        assertThat(account.getSyncStatus()).isEqualTo(IdentitySyncStatus.FAILED);
        verify(keycloak, never()).revokeSessions(anyString());
        verify(accounts).save(account);
        verify(jobs).save(job);
    }

    @Test
    void aJobForAVersionTheAccountHasMovedPastIsClosedWithoutCallingKeycloak() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "user-1", 6L, NOW);
        when(jobs.lockDue(NOW)).thenReturn(List.of(job));
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account(7L)));

        reconciler.reconcile();

        assertThat(job.getStatus()).isEqualTo(KeycloakSyncJob.Status.SUCCEEDED);
        verify(jobs).save(job);
        verify(accounts, never()).save(any());
        verify(keycloak, never()).synchronizeAuthorization(anyString(), anyString(), anyBoolean(), anySet(), anyLong(), anyLong(), any());
    }

    @Test
    void aJobWhoseAccountIsGoneIsClosedToo() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "ghost", 6L, NOW);
        when(jobs.lockDue(NOW)).thenReturn(List.of(job));
        when(accounts.lockById("ghost")).thenReturn(Optional.empty());

        reconciler.reconcile();

        assertThat(job.getStatus()).isEqualTo(KeycloakSyncJob.Status.SUCCEEDED);
        verify(jobs).save(job);
    }

    @Test
    void atMostFiftyJobsAreHandledInOneRound() {
        List<KeycloakSyncJob> due = new ArrayList<>(IntStream.range(0, 60)
                .mapToObj(i -> KeycloakSyncJob.pending("job-" + i, "user-" + i, 6L, NOW)).toList());
        when(jobs.lockDue(NOW)).thenReturn(due);
        when(accounts.lockById(anyString())).thenReturn(Optional.empty());

        reconciler.reconcile();

        verify(accounts, org.mockito.Mockito.times(50)).lockById(anyString());
    }

    private static IdentityAccount account(long version, RoleGrant... grants) {
        return IdentityAccount.builder().userId("user-1").keycloakSubject("kc-1")
                .status(IdentityStatus.ACTIVE).syncStatus(IdentitySyncStatus.PENDING)
                .authorizationVersion(version).authorizationPolicyVersion(2L)
                .createdAt(NOW.minusDays(1)).updatedAt(NOW.minusDays(1)).grants(new ArrayList<>(List.of(grants))).build();
    }

    private static RoleGrant grant(AuthorizationRole role, LocalDateTime expiresAt) {
        return RoleGrant.builder().roleCode(role).grantedBy("test").grantedAt(NOW.minusDays(1)).expiresAt(expiresAt)
                .status(AuthorizationGrantStatus.ACTIVE).source("TEST").build();
    }
}
