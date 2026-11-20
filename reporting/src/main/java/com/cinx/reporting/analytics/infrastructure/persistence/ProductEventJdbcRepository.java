package com.cinx.reporting.analytics.infrastructure.persistence;

import com.cinx.reporting.analytics.domain.ProductEvent;
import com.cinx.reporting.analytics.domain.ProductEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Raw events go through plain SQL: INSERT IGNORE is the deduplication, and no JPA entity is worth it for an append-only table. */
@Component
@RequiredArgsConstructor
class ProductEventJdbcRepository implements ProductEventRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    @Override
    public int insertNew(List<ProductEvent> events) {
        int inserted = 0;
        for (ProductEvent event : events) {
            inserted += jdbc.update("""
                            INSERT IGNORE INTO product_event (user_id, event_id, name, occurred_at, received_at, props)
                            VALUES (?, ?, ?, ?, ?, CAST(? AS JSON))""",
                    event.userId(), event.eventId(), event.name(), Timestamp.valueOf(event.occurredAt()),
                    Timestamp.valueOf(event.receivedAt()), write(event));
        }
        return inserted;
    }

    @Override
    public boolean recordFirstExposure(String experimentKey, String userId, String variant, LocalDateTime exposedAt) {
        return jdbc.update("""
                        INSERT IGNORE INTO experiment_exposure (experiment_key, user_id, variant, exposed_at)
                        VALUES (?, ?, ?, ?)""",
                experimentKey, userId, variant, Timestamp.valueOf(exposedAt)) == 1;
    }

    @Override
    public void rollUpDay(LocalDate day, LocalDateTime from, LocalDateTime to) {
        jdbc.update("""
                        INSERT INTO product_event_daily (day, name, events, users)
                        SELECT ?, name, COUNT(*), COUNT(DISTINCT user_id) FROM product_event
                        WHERE occurred_at >= ? AND occurred_at < ? GROUP BY name
                        ON DUPLICATE KEY UPDATE events = VALUES(events), users = VALUES(users)""",
                Date.valueOf(day), Timestamp.valueOf(from), Timestamp.valueOf(to));
    }

    @Override
    public int deleteOlderThan(LocalDateTime before, int limit) {
        return jdbc.update("DELETE FROM product_event WHERE occurred_at < ? LIMIT ?", Timestamp.valueOf(before), limit);
    }

    private String write(ProductEvent event) {
        try {
            return json.writeValueAsString(event.props());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Event props could not be written", e);
        }
    }
}
