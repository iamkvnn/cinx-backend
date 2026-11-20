package com.cinx.reporting.analytics.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsConsentRepositoryAdapterTest {
    private static final LocalDateTime T = LocalDateTime.of(2026, 10, 13, 9, 0);

    @Mock
    private AnalyticsConsentJpaRepository jpa;

    @Test
    void noRowMeansOn() {
        when(jpa.findById("u1")).thenReturn(Optional.empty());

        assertThat(new AnalyticsConsentRepositoryAdapter(jpa).allows("u1")).isTrue();
    }

    @Test
    void anOlderSnapshotNeverUndoesANewerSwitch() {
        AnalyticsConsentJpaEntity stored = new AnalyticsConsentJpaEntity();
        stored.setUserId("u1");
        stored.setEnabled(false);
        stored.setUpdatedAt(T);
        when(jpa.findById("u1")).thenReturn(Optional.of(stored));
        AnalyticsConsentRepositoryAdapter adapter = new AnalyticsConsentRepositoryAdapter(jpa);

        adapter.record("u1", true, T.minusMinutes(1));
        verify(jpa, never()).save(any());

        adapter.record("u1", true, T.plusMinutes(1));
        verify(jpa).save(stored);
        assertThat(stored.isEnabled()).isTrue();
    }
}
