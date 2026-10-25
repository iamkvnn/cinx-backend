package com.cinx.auth.identity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** A pending push of one account's authorization version to Keycloak, retried with a growing delay until it works or gives up. */
@Getter
@Builder
@AllArgsConstructor
public class KeycloakSyncJob {
    public enum Status { PENDING, RETRYABLE_FAILED, SUCCEEDED, DEAD }

    public static final int MAX_ATTEMPTS = 10;
    private static final long MAX_DELAY_SECONDS = 300;

    private final String id;
    private final String userId;
    private final long desiredAuthorizationVersion;
    private Status status;
    private int attemptCount;
    private LocalDateTime nextAttemptAt;
    private String lastErrorCode;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KeycloakSyncJob pending(String id, String userId, long desiredAuthorizationVersion, LocalDateTime now) {
        return KeycloakSyncJob.builder().id(id).userId(userId).desiredAuthorizationVersion(desiredAuthorizationVersion)
                .status(Status.PENDING).attemptCount(0).nextAttemptAt(now).createdAt(now).updatedAt(now).build();
    }

    /** The account moved on (or vanished) since this job was queued, so there is nothing left to push. */
    public void markSuperseded() {
        status = Status.SUCCEEDED;
    }

    public void markSucceeded(LocalDateTime now) {
        status = Status.SUCCEEDED;
        lastErrorCode = null;
        updatedAt = now;
    }

    /** One more failed try: retry after 2^attempts seconds (at most five minutes), or give up after {@link #MAX_ATTEMPTS}. */
    public void markFailed(LocalDateTime now, String errorCode) {
        attemptCount = attemptCount + 1;
        status = attemptCount >= MAX_ATTEMPTS ? Status.DEAD : Status.RETRYABLE_FAILED;
        nextAttemptAt = now.plusSeconds(Math.min(MAX_DELAY_SECONDS, 1L << Math.min(attemptCount, 8)));
        lastErrorCode = errorCode;
        updatedAt = now;
    }
}
