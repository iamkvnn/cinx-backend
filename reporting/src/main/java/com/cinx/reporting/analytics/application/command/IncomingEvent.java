package com.cinx.reporting.analytics.application.command;

import java.time.OffsetDateTime;
import java.util.Map;

/** One event as the client sent it, before the dictionary checks it. */
public record IncomingEvent(String eventId, String name, OffsetDateTime occurredAt, Map<String, Object> props) {
}
