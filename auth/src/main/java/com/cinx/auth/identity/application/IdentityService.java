package com.cinx.auth.identity.application;

import com.cinx.auth.identity.application.command.BanUserCommand;
import com.cinx.auth.identity.application.command.UpdateStaffRolesCommand;
import com.cinx.auth.identity.application.query.AuthorizationState;
import com.cinx.auth.identity.application.query.IdentityAccountView;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityAccountRepository;
import com.cinx.auth.identity.domain.IdentityStatus;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.common.logging.CorrelationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keycloak owns roles; this service changes them there synchronously, then records the change (status, audit, event)
 * in the same transaction. A Keycloak failure throws before anything is stored.
 */
@Service
@RequiredArgsConstructor
public class IdentityService implements IIdentityService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<AuthorizationRole> WORKFLOW_ROLES = EnumSet.of(AuthorizationRole.LEARNER,
            AuthorizationRole.INSTRUCTOR, AuthorizationRole.PLATFORM_ADMIN, AuthorizationRole.BREAK_GLASS_ADMIN);

    private final IdentityAccountRepository accounts;
    private final KeycloakAdminClient keycloak;
    private final AuthorizationAuditLog audits;
    private final IdentityEvents events;
    private final AuthorizationCatalog catalog;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public IdentityAccountView findAccount(String userId) {
        KeycloakAdminClient.KeycloakUser user = keycloak.findUser(userId)
                .orElseThrow(() -> new NotFoundException("Identity account not found"));
        IdentityAccount account = accounts.findById(userId).orElseGet(() -> IdentityAccount.create(userId, now()));
        return toView(account, user.email(), keycloak.effectiveRoles(userId));
    }

    @Override
    @Transactional
    public IdentityAccountView ban(String userId, BanUserCommand command) {
        IdentityAccount account = lockOrCreate(userId);
        keycloak.setEnabled(userId, false);
        account.setStatus(IdentityStatus.BANNED);
        account.setBanReason(command.reasonType().name());
        account.setBanDetails(command.details());
        account.setBanExpiresAt(command.durationDays() == null ? null : now().plusDays(command.durationDays()));
        return persistChange(account, "ACCOUNT_SUSPENDED", null);
    }

    @Override
    @Transactional
    public IdentityAccountView unban(String userId) {
        IdentityAccount account = lockOrCreate(userId);
        keycloak.setEnabled(userId, true);
        account.setStatus(IdentityStatus.ACTIVE);
        account.setBanReason(null);
        account.setBanDetails(null);
        account.setBanExpiresAt(null);
        return persistChange(account, "ACCOUNT_REACTIVATED", null);
    }

    @Override
    @Transactional
    public IdentityAccountView promoteInstructor(String userId) {
        IdentityAccount account = lockOrCreate(userId);
        if (keycloak.directRoles(userId).contains(AuthorizationRole.INSTRUCTOR)) return currentView(account);
        keycloak.addRoles(userId, Set.of(AuthorizationRole.INSTRUCTOR));
        return persistChange(account, "ROLE_GRANTED", AuthorizationRole.INSTRUCTOR);
    }

    @Override
    @Transactional
    public IdentityAccountView terminateInstructor(String userId) {
        IdentityAccount account = lockOrCreate(userId);
        if (!keycloak.directRoles(userId).contains(AuthorizationRole.INSTRUCTOR)) return currentView(account);
        keycloak.removeRoles(userId, Set.of(AuthorizationRole.INSTRUCTOR));
        return persistChange(account, "ROLE_REVOKED", AuthorizationRole.INSTRUCTOR);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorizationState authorizationState(String userId) {
        IdentityAccount account = accounts.findById(userId).orElse(null);
        if (account == null) return new AuthorizationState(userId, IdentityStatus.ACTIVE, null, clock.instant());
        return new AuthorizationState(userId, account.getStatus(),
                account.getChangedAt() == null ? null : account.getChangedAt().toInstant(ZoneOffset.UTC), clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findUserIdsByPermission(String permission) {
        if (!permission.startsWith("learning.") && !permission.startsWith("notification.")) {
            throw new IllegalArgumentException("Permission is not allowlisted for recipient lookup");
        }
        Set<String> userIds = new LinkedHashSet<>();
        catalog.roles().stream()
                .filter(role -> catalog.effectivePermissions(Set.of(role)).contains(permission))
                .forEach(role -> userIds.addAll(keycloak.enabledUsersWithRole(role)));
        return List.copyOf(userIds);
    }

    @Override
    @Transactional
    public IdentityAccountView updateStaffRoles(String actorId, String userId, UpdateStaffRolesCommand command) {
        if (actorId.equals(userId)) throw new BadRequestException(ErrorCode.BAD_REQUEST, "Self role assignment is not allowed");
        if (command.roles().stream().anyMatch(WORKFLOW_ROLES::contains)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Requested role requires a dedicated approval workflow");
        }
        IdentityAccount account = lockOrCreate(userId);
        Set<AuthorizationRole> held = keycloak.directRoles(userId);
        Set<AuthorizationRole> toAdd = EnumSet.noneOf(AuthorizationRole.class);
        Set<AuthorizationRole> toRemove = EnumSet.noneOf(AuthorizationRole.class);
        for (AuthorizationRole role : AuthorizationRole.values()) {
            if (WORKFLOW_ROLES.contains(role)) continue;
            boolean wanted = command.roles().contains(role);
            if (wanted && !held.contains(role)) toAdd.add(role);
            if (!wanted && held.contains(role)) toRemove.add(role);
        }
        if (toAdd.isEmpty() && toRemove.isEmpty()) return currentView(account);
        keycloak.addRoles(userId, toAdd);
        keycloak.removeRoles(userId, toRemove);
        return persistChange(account, "STAFF_ROLE_SET_UPDATED", null);
    }

    private IdentityAccountView persistChange(IdentityAccount account, String action, AuthorizationRole role) {
        account.markChanged(now());
        accounts.save(account);
        Set<AuthorizationRole> effective = keycloak.effectiveRoles(account.getUserId());
        audits.append(new AuthorizationAuditEntry(UUID.randomUUID().toString(), "SYSTEM", "auth", account.getUserId(), action,
                role == null ? null : role.name(), snapshot(account, effective), catalog.policyVersion(),
                CorrelationContext.currentRequestId(), now()));
        events.authorizationChanged(account, effective);
        return toView(account, emailOf(account.getUserId()), effective);
    }

    private String snapshot(IdentityAccount account, Set<AuthorizationRole> effective) {
        try {
            return JSON.writeValueAsString(Map.of(
                    "status", account.getStatus().name(),
                    "roles", effective.stream().map(Enum::name).sorted().toList(),
                    "changedAt", account.getChangedAt().toString()));
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException("Unable to serialize authorization audit snapshot", impossible);
        }
    }

    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }

    /** The account row, created on first use; the user must exist in Keycloak. */
    private IdentityAccount lockOrCreate(String userId) {
        return accounts.lockById(userId).orElseGet(() -> {
            keycloak.findUser(userId).orElseThrow(() -> new NotFoundException("Identity account not found"));
            return IdentityAccount.create(userId, now());
        });
    }

    private IdentityAccountView currentView(IdentityAccount account) {
        return toView(account, emailOf(account.getUserId()), keycloak.effectiveRoles(account.getUserId()));
    }

    private String emailOf(String userId) {
        return keycloak.findUser(userId).map(KeycloakAdminClient.KeycloakUser::email).orElse(null);
    }

    private IdentityAccountView toView(IdentityAccount account, String email, Set<AuthorizationRole> roles) {
        return new IdentityAccountView(account.getUserId(), email, account.getStatus(),
                roles.stream().map(Enum::name).sorted().toList(), account.getBanExpiresAt(), account.getChangedAt());
    }
}
