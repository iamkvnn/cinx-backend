package com.cinx.reporting.analytics.application;

import com.cinx.reporting.analytics.domain.ProductEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsRetentionServiceTest {
    // 02:00 in Vietnam on the 13th
    private static final Instant NOW = Instant.parse("2026-10-12T19:00:00Z");

    @Mock
    private ProductEventRepository events;

    @Test
    void theLastThreeVietnamDaysAreRolledUpBeforeRawEventsPastOneHundredEightyDaysGo() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 12, 19, 0).minusDays(180);
        when(events.deleteOlderThan(cutoff, AnalyticsRetentionService.DELETE_CHUNK))
                .thenReturn(AnalyticsRetentionService.DELETE_CHUNK, 12);

        new AnalyticsRetentionService(events, Clock.fixed(NOW, ZoneOffset.UTC)).run();

        InOrder order = inOrder(events);
        order.verify(events).rollUpDay(LocalDate.of(2026, 10, 10), LocalDateTime.of(2026, 10, 9, 17, 0), LocalDateTime.of(2026, 10, 10, 17, 0));
        order.verify(events).rollUpDay(LocalDate.of(2026, 10, 11), LocalDateTime.of(2026, 10, 10, 17, 0), LocalDateTime.of(2026, 10, 11, 17, 0));
        order.verify(events).rollUpDay(LocalDate.of(2026, 10, 12), LocalDateTime.of(2026, 10, 11, 17, 0), LocalDateTime.of(2026, 10, 12, 17, 0));
        order.verify(events, times(2)).deleteOlderThan(cutoff, AnalyticsRetentionService.DELETE_CHUNK);
        verify(events, times(3)).rollUpDay(any(), any(), any());
        verify(events, times(2)).deleteOlderThan(any(), anyInt());
    }
}
