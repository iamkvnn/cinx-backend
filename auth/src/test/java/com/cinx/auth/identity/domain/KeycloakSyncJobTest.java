package com.cinx.auth.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakSyncJobTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Test
    void aNewJobIsPendingAndDueImmediately() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "user-1", 5L, NOW);

        assertThat(job.getStatus()).isEqualTo(KeycloakSyncJob.Status.PENDING);
        assertThat(job.getAttemptCount()).isZero();
        assertThat(job.getNextAttemptAt()).isEqualTo(NOW);
        assertThat(job.getDesiredAuthorizationVersion()).isEqualTo(5L);
        assertThat(job.getLastErrorCode()).isNull();
    }

    @Test
    void successClearsTheLastErrorAndStampsTheTime() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "user-1", 5L, NOW);
        job.markFailed(NOW, "KeycloakSyncException");

        job.markSucceeded(NOW.plusMinutes(1));

        assertThat(job.getStatus()).isEqualTo(KeycloakSyncJob.Status.SUCCEEDED);
        assertThat(job.getLastErrorCode()).isNull();
        assertThat(job.getUpdatedAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    void aSupersededJobIsDoneWithoutTouchingItsOtherFields() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "user-1", 5L, NOW);

        job.markSuperseded();

        assertThat(job.getStatus()).isEqualTo(KeycloakSyncJob.Status.SUCCEEDED);
        assertThat(job.getUpdatedAt()).isEqualTo(NOW);
        assertThat(job.getAttemptCount()).isZero();
    }

    @Test
    void eachFailureWaitsTwiceAsLongAsTheLastUpToTwoHundredFiftySixSeconds() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "user-1", 5L, NOW);
        long[] expectedSeconds = {2, 4, 8, 16, 32, 64, 128, 256, 256};

        for (int attempt = 1; attempt <= expectedSeconds.length; attempt++) {
            job.markFailed(NOW, "KeycloakSyncException");

            assertThat(job.getAttemptCount()).isEqualTo(attempt);
            assertThat(job.getStatus()).as("attempt %d", attempt).isEqualTo(KeycloakSyncJob.Status.RETRYABLE_FAILED);
            assertThat(job.getNextAttemptAt()).as("attempt %d", attempt).isEqualTo(NOW.plusSeconds(expectedSeconds[attempt - 1]));
        }
        assertThat(job.getLastErrorCode()).isEqualTo("KeycloakSyncException");
    }

    @Test
    void theTenthFailureGivesUp() {
        KeycloakSyncJob job = KeycloakSyncJob.pending("job-1", "user-1", 5L, NOW);

        for (int attempt = 1; attempt < KeycloakSyncJob.MAX_ATTEMPTS; attempt++) {
            job.markFailed(NOW, "X");
            assertThat(job.getStatus()).isEqualTo(KeycloakSyncJob.Status.RETRYABLE_FAILED);
        }
        job.markFailed(NOW, "X");

        assertThat(job.getAttemptCount()).isEqualTo(10);
        assertThat(job.getStatus()).isEqualTo(KeycloakSyncJob.Status.DEAD);
    }
}
