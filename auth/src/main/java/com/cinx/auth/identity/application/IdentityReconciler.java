package com.cinx.auth.identity.application;

import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityAccountRepository;
import com.cinx.auth.identity.domain.IdentityStatus;
import com.cinx.auth.identity.domain.IdentitySyncStatus;
import com.cinx.auth.identity.domain.KeycloakSyncJob;
import com.cinx.auth.identity.domain.KeycloakSyncJobRepository;
import com.cinx.auth.identity.domain.RoleGrant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityReconciler {
    private final IdentityAccountRepository accounts;
    private final KeycloakSyncJobRepository jobs;
    private final KeycloakAdminClient keycloak;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${identity.reconciliation.fixed-delay-ms:5000}")
    @Transactional
    public void reconcile() {
        LocalDateTime now = now();
        for (KeycloakSyncJob job : jobs.lockDue(now).stream().limit(50).toList()) {
            IdentityAccount account = accounts.lockById(job.getUserId()).orElse(null);
            if (account == null || account.getAuthorizationVersion() != job.getDesiredAuthorizationVersion()) {
                job.markSuperseded();
                jobs.save(job);
                continue;
            }
            Set<AuthorizationRole> roleSet = account.effectiveRoles(now);
            Instant notAfter = account.effectiveGrants(now).stream().map(RoleGrant::getExpiresAt)
                    .filter(java.util.Objects::nonNull).min(LocalDateTime::compareTo)
                    .map(value -> value.toInstant(ZoneOffset.UTC)).orElse(null);
            try {
                keycloak.synchronizeAuthorization(subject(account), account.getUserId(),
                        account.getStatus() == IdentityStatus.ACTIVE, roleSet,
                        account.getAuthorizationVersion(), account.getAuthorizationPolicyVersion(), notAfter);
                keycloak.revokeSessions(subject(account));
                job.markSucceeded(now);
                account.setSyncStatus(IdentitySyncStatus.SYNCED);
            } catch (RuntimeException failure) {
                job.markFailed(now, failure.getClass().getSimpleName());
                account.setSyncStatus(IdentitySyncStatus.FAILED);
                log.warn("Keycloak authorization reconciliation failed userId={} version={}",
                        account.getUserId(), account.getAuthorizationVersion());
            }
            account.setUpdatedAt(now);
            accounts.save(account);
            jobs.save(job);
        }
    }

    private String subject(IdentityAccount account) {
        return account.getKeycloakSubject() == null ? account.getUserId() : account.getKeycloakSubject();
    }

    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
}
