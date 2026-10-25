package com.cinx.auth.identity.application;

/** Port: the append-only audit trail of authorization changes. */
public interface AuthorizationAuditLog {
    void append(AuthorizationAuditEntry entry);
}
