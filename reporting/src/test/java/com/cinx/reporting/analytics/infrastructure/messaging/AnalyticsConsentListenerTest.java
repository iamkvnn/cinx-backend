package com.cinx.reporting.analytics.infrastructure.messaging;

import com.cinx.reporting.analytics.domain.AnalyticsConsentRepository;
import com.cinx.reporting.shared.infrastructure.messaging.InboxMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsConsentListenerTest {
    private static final Instant NOW = Instant.parse("2026-10-13T09:00:00Z");

    @Mock
    private AnalyticsConsentRepository consents;
    @Mock
    private InboxMessageRepository inbox;
    private AnalyticsConsentListener listener;

    @BeforeEach
    void setUp() {
        listener = new AnalyticsConsentListener(consents, inbox, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void theSwitchIsKeptAtTheEventsTime() {
        listener.learningProfileUpdated(Map.of("userId", "u1", "usageAnalyticsConsent", false,
                "occurredAt", "2026-10-13T15:30:00+07:00"), "m1");

        verify(consents).record("u1", false, LocalDateTime.of(2026, 10, 13, 8, 30));
    }

    @Test
    void aProducerOlderThanTheSwitchMeansOn() {
        listener.learningProfileUpdated(Map.of("userId", "u1", "occurredAt", "2026-10-13T08:00:00Z"), "m1");

        verify(consents).record("u1", true, LocalDateTime.of(2026, 10, 13, 8, 0));
    }

    @Test
    void aRedeliveredEventIsANoOp() {
        when(inbox.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        listener.learningProfileUpdated(Map.of("userId", "u1", "usageAnalyticsConsent", false), "m1");

        verifyNoInteractions(consents);
    }
}
