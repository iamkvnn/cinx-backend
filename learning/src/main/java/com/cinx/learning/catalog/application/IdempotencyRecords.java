package com.cinx.learning.catalog.application;

import java.util.Optional;

/** Port: the Idempotency-Key records of catalog creates, one per user and key, with the first response. */
public interface IdempotencyRecords {
    Optional<Stored> find(String userId, String key);

    void save(String userId, String key, Stored record);

    record Stored(String operation, String requestHash, String resourceId, String responseJson) {
    }
}
