package com.cinx.reporting.analytics.domain;

import java.time.LocalDateTime;
import java.util.Map;

/** One accepted product event; times are UTC. */
public record ProductEvent(String userId, String eventId, String name, LocalDateTime occurredAt, LocalDateTime receivedAt,
                           Map<String, Object> props) {
}
