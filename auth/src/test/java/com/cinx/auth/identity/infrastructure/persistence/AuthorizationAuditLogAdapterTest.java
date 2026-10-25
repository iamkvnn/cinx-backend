package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.application.AuthorizationAuditEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthorizationAuditLogAdapterTest {
    @Mock AuthorizationAuditRepository audits;

    @Test
    void appendStoresEveryFieldOfTheEntryAndLeavesTheOptionalOnesEmpty() {
        LocalDateTime at = LocalDateTime.of(2026, 8, 1, 12, 0);

        new AuthorizationAuditLogAdapter(audits).append(new AuthorizationAuditEntry("audit-1", "SYSTEM", "auth", "user-1",
                "ROLE_GRANTED", "INSTRUCTOR", "{\"status\":\"ACTIVE\"}", 3L, "req-1", at));

        ArgumentCaptor<AuthorizationAudit> stored = ArgumentCaptor.forClass(AuthorizationAudit.class);
        verify(audits).save(stored.capture());
        AuthorizationAudit row = stored.getValue();
        assertThat(row.getId()).isEqualTo("audit-1");
        assertThat(row.getActorType()).isEqualTo("SYSTEM");
        assertThat(row.getActorId()).isEqualTo("auth");
        assertThat(row.getTargetUserId()).isEqualTo("user-1");
        assertThat(row.getAction()).isEqualTo("ROLE_GRANTED");
        assertThat(row.getRoleCode()).isEqualTo("INSTRUCTOR");
        assertThat(row.getAfterJson()).isEqualTo("{\"status\":\"ACTIVE\"}");
        assertThat(row.getAuthorizationPolicyVersion()).isEqualTo(3L);
        assertThat(row.getRequestId()).isEqualTo("req-1");
        assertThat(row.getOccurredAt()).isEqualTo(at);
        assertThat(row.getBeforeJson()).isNull();
        assertThat(row.getTraceId()).isNull();
    }
}
