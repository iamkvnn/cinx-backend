package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.application.AuthorizationAuditEntry;
import com.cinx.auth.identity.application.AuthorizationAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AuthorizationAuditLogAdapter implements AuthorizationAuditLog {
    private final AuthorizationAuditRepository audits;

    @Override
    public void append(AuthorizationAuditEntry entry) {
        audits.save(AuthorizationAudit.builder().id(entry.id())
                .actorType(entry.actorType()).actorId(entry.actorId())
                .targetUserId(entry.targetUserId()).action(entry.action())
                .roleCode(entry.roleCode()).afterJson(entry.afterJson())
                .authorizationPolicyVersion(entry.authorizationPolicyVersion())
                .requestId(entry.requestId()).occurredAt(entry.occurredAt()).build());
    }
}
