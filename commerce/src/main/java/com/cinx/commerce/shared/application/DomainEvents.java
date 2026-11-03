package com.cinx.commerce.shared.application;

import java.util.Map;

/** Port: records an event in the outbox, in the caller's transaction, for the relay to publish after the commit. */
public interface DomainEvents {
    /** Publishes on the shared domain exchange with the routing key the event type implies. */
    void publish(String eventType, String aggregateId, Map<String, Object> payload);

    void publish(String exchange, String routingKey, int eventVersion, String eventType,
                 String aggregateId, Map<String, Object> payload, Map<String, Object> headers);
}
