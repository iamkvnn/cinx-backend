package com.cinx.auth.identity.application;

import java.time.LocalDateTime;

/** One line of the authorization audit trail: who did what to whom, and the state afterwards. */
public record AuthorizationAuditEntry(
        String id,
        String actorType,
        String actorId,
        String targetUserId,
        String action,
        String roleCode,
        String afterJson,
        long authorizationPolicyVersion,
        String requestId,
        LocalDateTime occurredAt) {
}
