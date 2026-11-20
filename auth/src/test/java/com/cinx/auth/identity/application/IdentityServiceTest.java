package com.cinx.auth.identity.application;

import com.cinx.auth.identity.application.command.BanUserCommand;
import com.cinx.auth.identity.application.command.UpdateStaffRolesCommand;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.BanReasonType;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityAccountRepository;
import com.cinx.auth.identity.domain.IdentityStatus;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 0, 0);
    static final KeycloakAdminClient.KeycloakUser USER = new KeycloakAdminClient.KeycloakUser("user-1", "u@example.com", true);

    @Mock IdentityAccountRepository accounts;
    @Mock KeycloakAdminClient keycloak;
    @Mock AuthorizationAuditLog audits;
    @Mock IdentityEvents events;
    @Mock AuthorizationCatalog catalog;
    IdentityService service;

    @BeforeEach
    void setUp() {
        service = new IdentityService(accounts, keycloak, audits, events, catalog,
                Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC));
        lenient().when(keycloak.findUser("user-1")).thenReturn(Optional.of(USER));
        lenient().when(keycloak.effectiveRoles("user-1")).thenReturn(EnumSet.of(AuthorizationRole.LEARNER));
    }

    @Test
    void aUserNobodyTouchedHasActiveStateAndNoChange() {
        when(accounts.findById("user-1")).thenReturn(Optional.empty());

        var state = service.authorizationState("user-1");

        assertThat(state.status()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(state.changedAt()).isNull();
        verifyNoInteractions(keycloak);
    }

    @Test
    void authorizationStateCarriesTheLastChangeAsAnInstant() {
        when(accounts.findById("user-1")).thenReturn(Optional.of(account(IdentityStatus.BANNED, NOW.minusHours(1))));

        var state = service.authorizationState("user-1");

        assertThat(state.status()).isEqualTo(IdentityStatus.BANNED);
        assertThat(state.changedAt()).isEqualTo(Instant.parse("2026-07-31T23:00:00Z"));
    }

    @Test
    void theFirstChangeCreatesTheAccountRow() {
        when(accounts.lockById("user-1")).thenReturn(Optional.empty());
        when(keycloak.directRoles("user-1")).thenReturn(EnumSet.noneOf(AuthorizationRole.class));

        service.promoteInstructor("user-1");

        ArgumentCaptor<IdentityAccount> saved = ArgumentCaptor.forClass(IdentityAccount.class);
        verify(accounts).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo("user-1");
        assertThat(saved.getValue().getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(saved.getValue().getChangedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void aChangeForAUserKeycloakDoesNotKnowIsNotFoundAndStoresNothing() {
        when(accounts.lockById("ghost")).thenReturn(Optional.empty());
        when(keycloak.findUser("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.promoteInstructor("ghost")).isInstanceOf(NotFoundException.class);

        verify(accounts, never()).save(any());
        verify(keycloak, never()).addRoles(anyString(), any());
    }

    @Test
    void instructorGrantAddsTheRoleInKeycloakThenAuditsAndAnnouncesIt() {
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account(IdentityStatus.ACTIVE, null)));
        when(keycloak.directRoles("user-1")).thenReturn(EnumSet.of(AuthorizationRole.LEARNER));
        when(keycloak.effectiveRoles("user-1")).thenReturn(EnumSet.of(AuthorizationRole.LEARNER, AuthorizationRole.INSTRUCTOR));

        var view = service.promoteInstructor("user-1");

        verify(keycloak).addRoles("user-1", Set.of(AuthorizationRole.INSTRUCTOR));
        ArgumentCaptor<AuthorizationAuditEntry> audit = ArgumentCaptor.forClass(AuthorizationAuditEntry.class);
        verify(audits).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("ROLE_GRANTED");
        assertThat(audit.getValue().roleCode()).isEqualTo("INSTRUCTOR");
        verify(events).authorizationChanged(any(), eq(EnumSet.of(AuthorizationRole.LEARNER, AuthorizationRole.INSTRUCTOR)));
        assertThat(view.roles()).containsExactly("INSTRUCTOR", "LEARNER");
        assertThat(view.changedAt()).isEqualTo(NOW);
        assertThat(view.email()).isEqualTo("u@example.com");
    }

    @Test
    void instructorGrantIsIdempotentWhenKeycloakAlreadyHasTheRole() {
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account(IdentityStatus.ACTIVE, null)));
        when(keycloak.directRoles("user-1")).thenReturn(EnumSet.of(AuthorizationRole.INSTRUCTOR));

        service.promoteInstructor("user-1");

        verify(keycloak, never()).addRoles(anyString(), any());
        verify(accounts, never()).save(any());
        verifyNoInteractions(audits, events);
    }

    @Test
    void terminatingSomeoneWhoIsNotAnInstructorChangesNothing() {
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account(IdentityStatus.ACTIVE, null)));
        when(keycloak.directRoles("user-1")).thenReturn(EnumSet.of(AuthorizationRole.LEARNER));

        service.terminateInstructor("user-1");

        verify(keycloak, never()).removeRoles(anyString(), any());
        verify(accounts, never()).save(any());
    }

    @Test
    void terminatingAnInstructorRemovesTheRoleAndMarksTheChange() {
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account(IdentityStatus.ACTIVE, null)));
        when(keycloak.directRoles("user-1")).thenReturn(EnumSet.of(AuthorizationRole.INSTRUCTOR));

        var view = service.terminateInstructor("user-1");

        verify(keycloak).removeRoles("user-1", Set.of(AuthorizationRole.INSTRUCTOR));
        assertThat(view.changedAt()).isEqualTo(NOW);
    }

    @Test
    void suspensionDisablesTheKeycloakUserAndRecordsTheBan() {
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account(IdentityStatus.ACTIVE, null)));

        var view = service.ban("user-1", new BanUserCommand(BanReasonType.SPAM, "links", 7));

        verify(keycloak).setEnabled("user-1", false);
        ArgumentCaptor<IdentityAccount> saved = ArgumentCaptor.forClass(IdentityAccount.class);
        verify(accounts).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(IdentityStatus.BANNED);
        assertThat(saved.getValue().getBanReason()).isEqualTo("SPAM");
        assertThat(saved.getValue().getBanExpiresAt()).isEqualTo(NOW.plusDays(7));
        assertThat(saved.getValue().getChangedAt()).isEqualTo(NOW);
        assertThat(view.status()).isEqualTo(IdentityStatus.BANNED);
    }

    @Test
    void aKeycloakFailureStoresNothingAndAnnouncesNothing() {
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account(IdentityStatus.ACTIVE, null)));
        doThrow(new IllegalStateException("keycloak down")).when(keycloak).setEnabled("user-1", false);

        assertThatThrownBy(() -> service.ban("user-1", new BanUserCommand(BanReasonType.SPAM, "links", null)))
                .isInstanceOf(IllegalStateException.class);

        verify(accounts, never()).save(any());
        verifyNoInteractions(audits, events);
    }

    @Test
    void reactivationEnablesTheKeycloakUserAndClearsTheBan() {
        IdentityAccount banned = account(IdentityStatus.BANNED, NOW.minusDays(1));
        banned.setBanReason("SPAM");
        banned.setBanExpiresAt(NOW.plusDays(3));
        when(accounts.lockById("user-1")).thenReturn(Optional.of(banned));

        service.unban("user-1");

        verify(keycloak).setEnabled("user-1", true);
        assertThat(banned.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(banned.getBanReason()).isNull();
        assertThat(banned.getBanExpiresAt()).isNull();
        assertThat(banned.getChangedAt()).isEqualTo(NOW);
    }

    @Test
    void staffRolesAreAddedAndTheUnlistedOnesRemovedLeavingWorkflowRolesAlone() {
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account(IdentityStatus.ACTIVE, null)));
        when(keycloak.directRoles("user-1")).thenReturn(EnumSet.of(AuthorizationRole.INSTRUCTOR, AuthorizationRole.REPORT_ANALYST));

        service.updateStaffRoles("admin", "user-1", new UpdateStaffRolesCommand(Set.of(AuthorizationRole.SUPPORT_AGENT)));

        verify(keycloak).addRoles("user-1", EnumSet.of(AuthorizationRole.SUPPORT_AGENT));
        verify(keycloak).removeRoles("user-1", EnumSet.of(AuthorizationRole.REPORT_ANALYST));
        verify(accounts).save(any());
    }

    @Test
    void staffRolesThatAreAlreadyAsRequestedChangeNothing() {
        when(accounts.lockById("user-1")).thenReturn(Optional.of(account(IdentityStatus.ACTIVE, null)));
        when(keycloak.directRoles("user-1")).thenReturn(EnumSet.of(AuthorizationRole.SUPPORT_AGENT));

        service.updateStaffRoles("admin", "user-1", new UpdateStaffRolesCommand(Set.of(AuthorizationRole.SUPPORT_AGENT)));

        verify(keycloak, never()).addRoles(anyString(), any());
        verify(keycloak, never()).removeRoles(anyString(), any());
        verify(accounts, never()).save(any());
    }

    @Test
    void anAdminCannotChangeTheirOwnRoles() {
        assertThatThrownBy(() -> service.updateStaffRoles("user-1", "user-1",
                new UpdateStaffRolesCommand(Set.of(AuthorizationRole.SUPPORT_AGENT))))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(keycloak);
    }

    @Test
    void rolesThatNeedADedicatedWorkflowCannotBeSetThisWay() {
        assertThatThrownBy(() -> service.updateStaffRoles("admin", "user-1",
                new UpdateStaffRolesCommand(Set.of(AuthorizationRole.PLATFORM_ADMIN))))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(keycloak);
    }

    @Test
    void findAccountOfAUserWithoutARowReadsKeycloakAndShowsThemActive() {
        when(accounts.findById("user-1")).thenReturn(Optional.empty());

        var view = service.findAccount("user-1");

        assertThat(view.status()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(view.email()).isEqualTo("u@example.com");
        assertThat(view.roles()).containsExactly("LEARNER");
        assertThat(view.changedAt()).isNull();
    }

    @Test
    void findAccountOfAUserKeycloakDoesNotKnowIsNotFound() {
        when(keycloak.findUser("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAccount("ghost")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void permissionMembersAreTheEnabledHoldersOfEveryRoleThatGrantsIt() {
        when(catalog.roles()).thenReturn(Set.of(AuthorizationRole.CONTENT_REVIEWER, AuthorizationRole.PLATFORM_ADMIN,
                AuthorizationRole.LEARNER));
        when(catalog.effectivePermissions(any())).thenAnswer(call -> {
            Set<AuthorizationRole> roles = call.getArgument(0);
            return roles.contains(AuthorizationRole.LEARNER) ? Set.of() : Set.of("learning.course.review.any");
        });
        when(keycloak.enabledUsersWithRole(AuthorizationRole.CONTENT_REVIEWER)).thenReturn(List.of("r1", "both"));
        when(keycloak.enabledUsersWithRole(AuthorizationRole.PLATFORM_ADMIN)).thenReturn(List.of("both", "a1"));

        assertThat(service.findUserIdsByPermission("learning.course.review.any"))
                .containsExactlyInAnyOrder("r1", "both", "a1");
        verify(keycloak, never()).enabledUsersWithRole(AuthorizationRole.LEARNER);
    }

    @Test
    void permissionMembersAreOnlyAnsweredForAllowlistedPermissions() {
        assertThatThrownBy(() -> service.findUserIdsByPermission("finance.payout.approve"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static IdentityAccount account(IdentityStatus status, LocalDateTime changedAt) {
        return IdentityAccount.builder().userId("user-1").status(status).changedAt(changedAt)
                .createdAt(NOW.minusDays(10)).updatedAt(NOW.minusDays(10)).version(1L).build();
    }
}
