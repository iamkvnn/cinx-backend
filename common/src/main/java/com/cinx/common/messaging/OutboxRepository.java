package com.cinx.common.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface OutboxRepository<T extends OutboxMessageBase> extends JpaRepository<T, String> {
    boolean existsByEventId(String eventId);

    @Query(value = """
            SELECT * FROM outbox_message
            WHERE status = 'PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP(6)
            ORDER BY created_at
            LIMIT 50
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<T> lockPendingBatch();
}
