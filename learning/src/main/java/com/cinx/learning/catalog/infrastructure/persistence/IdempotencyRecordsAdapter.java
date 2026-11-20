package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.application.IdempotencyRecords;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class IdempotencyRecordsAdapter implements IdempotencyRecords {
    private final CatalogIdempotencyRecordJpaRepository jpa;
    private final Clock clock;

    @Override
    public Optional<Stored> find(String userId, String key) {
        return jpa.findByUserIdAndIdemKey(userId, key)
                .map(row -> new Stored(row.getOperation(), row.getRequestHash(), row.getResourceId(), row.getResponseJson()));
    }

    @Override
    public void save(String userId, String key, Stored record) {
        CatalogIdempotencyRecordJpaEntity row = new CatalogIdempotencyRecordJpaEntity();
        row.setId(UUID.randomUUID().toString());
        row.setUserId(userId);
        row.setIdemKey(key);
        row.setOperation(record.operation());
        row.setRequestHash(record.requestHash());
        row.setResourceId(record.resourceId());
        row.setResponseJson(record.responseJson());
        row.setCreatedAt(LocalDateTime.now(clock));
        // a concurrent first use of the same key fails on the unique key at commit, so only one create lands
        jpa.save(row);
    }
}
