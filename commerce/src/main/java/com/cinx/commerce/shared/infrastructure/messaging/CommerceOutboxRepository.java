package com.cinx.commerce.shared.infrastructure.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CommerceOutboxRepository extends JpaRepository<CommerceOutbox, String> {
    @Query(value = """
            SELECT * FROM commerce_outbox
            WHERE status = 'PENDING' AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP(6))
            ORDER BY created_at LIMIT 50 FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<CommerceOutbox> lockPendingBatch();
}
