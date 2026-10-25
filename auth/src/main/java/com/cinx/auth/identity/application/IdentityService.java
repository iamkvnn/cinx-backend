package com.cinx.auth.identity.application;

import com.cinx.auth.identity.application.command.BanUserCommand;
import com.cinx.auth.identity.application.command.LinkIdentityCommand;
import com.cinx.auth.identity.application.command.UpdateStaffRolesCommand;
import com.cinx.auth.identity.application.query.AccountAuthorizationQueries;
import com.cinx.auth.identity.application.query.AccountRoles;
import com.cinx.auth.identity.application.query.AuthorizationState;
import com.cinx.auth.identity.application.query.IdentityAccountView;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityAccountRepository;
import com.cinx.auth.identity.domain.IdentityStatus;
import com.cinx.auth.identity.domain.IdentitySyncStatus;
import com.cinx.auth.identity.domain.KeycloakSyncJob;
import com.cinx.auth.identity.domain.KeycloakSyncJobRepository;
import com.cinx.auth.identity.domain.RoleGrant;
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityService implements IIdentityService {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final IdentityAccountRepository accounts;
    private final KeycloakSyncJobRepository syncJobs;
    private final AuthorizationAuditLog audits;
    private final IdentityEvents events;
    private final AuthorizationCatalog catalog;
    private final AccountAuthorizationQueries queries;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public IdentityAccountView findAccount(String userId) {
        return toView(getAccount(userId));
    }

    @Override
    @Transactional
    public IdentityAccountView link(String userId, LinkIdentityCommand command) {
        LocalDateTime now = now();
        IdentityAccount account = accounts.lockById(userId).orElse(null);
        boolean created = account == null;
        if (created) {
            account = IdentityAccount.onboard(userId, catalog.policyVersion(), now);
        }
        account.setKeycloakSubject(command.keycloakSubject());
        account.setEmail(command.email());
        account.setUpdatedAt(now);
        boolean granted = account.grantRole(AuthorizationRole.LEARNER, "onboarding-system", "USER_PROFILE_ONBOARDED", null, now);
        if (!created && granted) account.incrementAuthorizationVersion();
        persistChange(account, created ? "ACCOUNT_ONBOARDED" : "ACCOUNT_RECONCILED", null);
        return toView(account);
    }

    @Override
    @Transactional
    public IdentityAccountView ban(String userId, BanUserCommand command) {
        IdentityAccount account = lockAccount(userId);
        if (account.getStatus() != IdentityStatus.BANNED) account.incrementAuthorizationVersion();
        account.setStatus(IdentityStatus.BANNED);
        account.setBanReason(command.reasonType().name());
        account.setBanDetails(command.details());
        account.setBanExpiresAt(command.durationDays() == null ? null : now().plusDays(command.durationDays()));
        account.setUpdatedAt(now());
        persistChange(account, "ACCOUNT_SUSPENDED", null);
        return toView(account);
    }

    @Override
    @Transactional
    public IdentityAccountView unban(String userId) {
        IdentityAccount account = lockAccount(userId);
        if (account.getStatus() != IdentityStatus.ACTIVE) account.incrementAuthorizationVersion();
        account.setStatus(IdentityStatus.ACTIVE);
        account.setBanReason(null);
        account.setBanDetails(null);
        account.setBanExpiresAt(null);
        account.setUpdatedAt(now());
        persistChange(account, "ACCOUNT_REACTIVATED", null);
        return toView(account);
    }

    @Override
    @Transactional
    public IdentityAccountView promoteInstructor(String userId) {
        IdentityAccount account = lockAccount(userId);
        if (account.grantRole(AuthorizationRole.INSTRUCTOR, "user-workflow", "INSTRUCTOR_VERIFIED", null, now())) {
            account.incrementAuthorizationVersion();
            persistChange(account, "ROLE_GRANTED", AuthorizationRole.INSTRUCTOR);
        }
        return toView(account);
    }

    @Override
    @Transactional
    public IdentityAccountView terminateInstructor(String userId) {
        IdentityAccount account = lockAccount(userId);
        if (account.revokeRole(AuthorizationRole.INSTRUCTOR, now())) {
            account.incrementAuthorizationVersion();
            persistChange(account, "ROLE_REVOKED", AuthorizationRole.INSTRUCTOR);
        }
        return toView(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorizationState authorizationState(String userId) {
        IdentityAccount account = getAccount(userId);
        Set<AuthorizationRole> roleSet = account.effectiveRoles(now());
        return new AuthorizationState(userId, account.getStatus(),
                roleSet.stream().map(Enum::name).sorted().toList(),
                account.getAuthorizationVersion(), account.getAuthorizationPolicyVersion(), clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findUserIdsByPermission(String permission) {
        if (!permission.startsWith("learning.") && !permission.startsWith("notification.")) {
            throw new IllegalArgumentException("Permission is not allowlisted for recipient lookup");
        }
        return queries.findActiveAccountRoles(now()).stream()
                .filter(account -> catalog.effectivePermissions(account.roles()).contains(permission))
                .map(AccountRoles::userId).toList();
    }

    @Override
    @Transactional
    public IdentityAccountView updateStaffRoles(String actorId, String userId, UpdateStaffRolesCommand command) {
        if (actorId.equals(userId)) throw new BadRequestException(ErrorCode.BAD_REQUEST, "Self role assignment is not allowed");
        Set<AuthorizationRole> forbidden = EnumSet.of(AuthorizationRole.LEARNER, AuthorizationRole.INSTRUCTOR,
                AuthorizationRole.PLATFORM_ADMIN, AuthorizationRole.BREAK_GLASS_ADMIN);
        if (!Collections.disjoint(command.roles(), forbidden)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Requested role requires a dedicated approval workflow");
        }
        LocalDateTime expiry = command.expiresAt() == null ? null
                : LocalDateTime.ofInstant(command.expiresAt(), java.time.ZoneOffset.UTC);
        if (expiry != null && !expiry.isAfter(now())) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Role expiry must be in the future");
        }
        IdentityAccount account = lockAccount(userId);
        boolean changed = false;
        for (AuthorizationRole role : AuthorizationRole.values()) {
            if (forbidden.contains(role)) continue;
            if (command.roles().contains(role)) {
                changed |= account.grantRole(role, actorId, "IDENTITY_ADMIN", expiry, now());
            } else if (account.revokeRole(role, now())) {
                changed = true;
            }
        }
        if (changed) {
            account.incrementAuthorizationVersion();
            persistChange(account, "STAFF_ROLE_SET_UPDATED", null);
        }
        return toView(account);
    }

    @Transactional
    void expireDueGrants() {
        for (IdentityAccount account : accounts.lockAccountsWithDueGrants(now())) {
            List<RoleGrant> expired = account.expireDueGrants(now());
            if (expired.isEmpty()) continue;
            expired.forEach(grant -> audits.append(new AuthorizationAuditEntry(UUID.randomUUID().toString(),
                    "SYSTEM", "authorization-expiry-worker", account.getUserId(), "ROLE_EXPIRED",
                    grant.getRoleCode().name(), "{\"status\":\"EXPIRED\"}", account.getAuthorizationPolicyVersion(),
                    null, now())));
            account.incrementAuthorizationVersion();
            persistChange(account, "AUTHORIZATION_EXPIRY_APPLIED", null);
        }
    }

    private void persistChange(IdentityAccount account, String action, AuthorizationRole role) {
        account.setSyncStatus(IdentitySyncStatus.PENDING);
        accounts.save(account);
        if (!syncJobs.existsByUserIdAndDesiredAuthorizationVersion(account.getUserId(), account.getAuthorizationVersion())) {
            syncJobs.save(KeycloakSyncJob.pending(UUID.randomUUID().toString(), account.getUserId(),
                    account.getAuthorizationVersion(), now()));
        }
        Set<AuthorizationRole> effective = account.effectiveRoles(now());
        audits.append(new AuthorizationAuditEntry(UUID.randomUUID().toString(), "SYSTEM", "auth", account.getUserId(), action,
                role == null ? null : role.name(), snapshot(account, effective), account.getAuthorizationPolicyVersion(),
                CorrelationContext.currentRequestId(), now()));
        events.authorizationChanged(account, effective);
    }

    private String snapshot(IdentityAccount account, Set<AuthorizationRole> effective) {
        try {
            return JSON.writeValueAsString(Map.of(
                    "status", account.getStatus().name(),
                    "roles", effective.stream().map(Enum::name).sorted().toList(),
                    "authorizationVersion", account.getAuthorizationVersion()));
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException("Unable to serialize authorization audit snapshot", impossible);
        }
    }

    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC); }

    private IdentityAccount lockAccount(String userId) { return accounts.lockById(userId).orElseThrow(() -> new NotFoundException("Identity account not found")); }

    private IdentityAccount getAccount(String userId) { return accounts.findById(userId).orElseThrow(() -> new NotFoundException("Identity account not found")); }

    private IdentityAccountView toView(IdentityAccount account) {
        return new IdentityAccountView(account.getUserId(), account.getKeycloakSubject(), account.getEmail(),
                account.getStatus(), account.getSyncStatus(), account.getBanExpiresAt());
    }
}
