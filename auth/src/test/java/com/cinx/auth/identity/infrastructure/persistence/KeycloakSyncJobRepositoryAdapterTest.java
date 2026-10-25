package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.KeycloakSyncJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakSyncJobRepositoryAdapterTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Mock KeycloakSyncJobJpaRepository jpa;
    KeycloakSyncJobPersistenceMapper mapper = Mappers.getMapper(KeycloakSyncJobPersistenceMapper.class);
    KeycloakSyncJobRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KeycloakSyncJobRepositoryAdapter(jpa, mapper);
    }

    @Test
    void aJobSurvivesTheTripThroughTheEntityWithEveryField() {
        KeycloakSyncJob original = KeycloakSyncJob.builder().id("job-1").userId("user-1").desiredAuthorizationVersion(7L)
                .status(KeycloakSyncJob.Status.RETRYABLE_FAILED).attemptCount(3).nextAttemptAt(NOW.plusSeconds(8))
                .lastErrorCode("KeycloakSyncException").createdAt(NOW.minusMinutes(5)).updatedAt(NOW).build();

        assertThat(mapper.toDomain(mapper.toEntity(original))).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void aNewJobIsInserted() {
        when(jpa.findById("job-1")).thenReturn(Optional.empty());
        when(jpa.save(any(KeycloakSyncJobJpaEntity.class))).thenAnswer(call -> call.getArgument(0));

        KeycloakSyncJob saved = adapter.save(KeycloakSyncJob.pending("job-1", "user-1", 7L, NOW));

        assertThat(saved.getId()).isEqualTo("job-1");
        assertThat(saved.getStatus()).isEqualTo(KeycloakSyncJob.Status.PENDING);
    }

    @Test
    void anExistingJobHasItsProgressWrittenBackAndNothingElse() {
        KeycloakSyncJobJpaEntity stored = KeycloakSyncJobJpaEntity.builder().id("job-1").userId("user-1").desiredAuthorizationVersion(7L)
                .status(KeycloakSyncJob.Status.PENDING).attemptCount(0).nextAttemptAt(NOW).createdAt(NOW.minusDays(1)).updatedAt(NOW.minusDays(1)).build();
        when(jpa.findById("job-1")).thenReturn(Optional.of(stored));
        when(jpa.save(stored)).thenReturn(stored);
        KeycloakSyncJob job = KeycloakSyncJob.builder().id("job-1").userId("tampered").desiredAuthorizationVersion(99L)
                .status(KeycloakSyncJob.Status.RETRYABLE_FAILED).attemptCount(1).nextAttemptAt(NOW.plusSeconds(2))
                .lastErrorCode("X").createdAt(NOW).updatedAt(NOW).build();

        adapter.save(job);

        assertThat(stored.getStatus()).isEqualTo(KeycloakSyncJob.Status.RETRYABLE_FAILED);
        assertThat(stored.getAttemptCount()).isEqualTo(1);
        assertThat(stored.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(stored.getLastErrorCode()).isEqualTo("X");
        assertThat(stored.getUserId()).isEqualTo("user-1");
        assertThat(stored.getDesiredAuthorizationVersion()).isEqualTo(7L);
        assertThat(stored.getCreatedAt()).isEqualTo(NOW.minusDays(1));
    }

    @Test
    void dueJobsComeBackAsDomainObjects() {
        when(jpa.lockDue(NOW)).thenReturn(List.of(KeycloakSyncJobJpaEntity.builder().id("job-1").userId("user-1")
                .desiredAuthorizationVersion(7L).status(KeycloakSyncJob.Status.PENDING).nextAttemptAt(NOW).createdAt(NOW).updatedAt(NOW).build()));

        assertThat(adapter.lockDue(NOW)).singleElement().satisfies(job -> assertThat(job.getId()).isEqualTo("job-1"));
    }

    @Test
    void existenceIsAskedByUserAndVersion() {
        when(jpa.existsByUserIdAndDesiredAuthorizationVersion("user-1", 8L)).thenReturn(true);

        assertThat(adapter.existsByUserIdAndDesiredAuthorizationVersion("user-1", 8L)).isTrue();
        assertThat(adapter.existsByUserIdAndDesiredAuthorizationVersion("user-1", 9L)).isFalse();
        verify(jpa).existsByUserIdAndDesiredAuthorizationVersion("user-1", 9L);
    }
}
