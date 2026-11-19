package com.cinx.reporting.reporting.infrastructure.messaging;

import com.cinx.reporting.reporting.infrastructure.persistence.AccountProjectionRepository;
import com.cinx.reporting.reporting.infrastructure.persistence.EnrollmentProjectionRepository;
import com.cinx.reporting.shared.infrastructure.messaging.InboxMessageRepository;
import com.cinx.reporting.reporting.infrastructure.persistence.OrderProjectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class ReportingEventListenerTest {
    @Test
    void duplicateInboxInsertDoesNotUpdateAccountProjection() {
        AccountProjectionRepository accounts = mock(AccountProjectionRepository.class);
        OrderProjectionRepository orders = mock(OrderProjectionRepository.class);
        EnrollmentProjectionRepository enrollments = mock(EnrollmentProjectionRepository.class);
        InboxMessageRepository inbox = mock(InboxMessageRepository.class);
        ReportingEventListener listener = new ReportingEventListener(accounts, orders, enrollments, inbox);
        when(inbox.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate event"));

        listener.auth(Map.of("userId", "user-1", "status", "ACTIVE"), "event-1");

        verifyNoInteractions(accounts, orders, enrollments);
    }

    @Test
    void authorizationSnapshotOnlyAppliesALaterChange() {
        AccountProjectionRepository accounts = mock(AccountProjectionRepository.class);
        OrderProjectionRepository orders = mock(OrderProjectionRepository.class);
        EnrollmentProjectionRepository enrollments = mock(EnrollmentProjectionRepository.class);
        InboxMessageRepository inbox = mock(InboxMessageRepository.class);
        ReportingEventListener listener = new ReportingEventListener(accounts, orders, enrollments, inbox);
        var account = com.cinx.reporting.reporting.infrastructure.persistence.AccountProjection.builder()
                .userId("user-1").status("ACTIVE").changedAt(java.time.LocalDateTime.of(2026, 8, 1, 0, 0)).build();
        when(accounts.findById("user-1")).thenReturn(Optional.of(account));

        listener.auth(Map.of("userId", "user-1", "status", "BANNED", "roles", List.of("LEARNER"),
                "changedAt", "2026-07-31T23:59:59Z"), "event-older");
        verify(accounts, never()).save(any());

        listener.auth(Map.of("userId", "user-1", "status", "BANNED", "roles", List.of("LEARNER"),
                "changedAt", "2026-08-01T00:00:01.250Z"), "event-newer");
        verify(accounts).save(account);
        org.assertj.core.api.Assertions.assertThat(account.getChangedAt())
                .isEqualTo(java.time.LocalDateTime.of(2026, 8, 1, 0, 0, 1, 250_000_000));
        org.assertj.core.api.Assertions.assertThat(account.getStatus()).isEqualTo("BANNED");
    }
}
