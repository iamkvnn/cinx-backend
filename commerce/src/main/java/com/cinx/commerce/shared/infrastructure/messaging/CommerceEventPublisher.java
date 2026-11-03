package com.cinx.commerce.shared.infrastructure.messaging;

import com.cinx.commerce.shared.application.DomainEvents;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class CommerceEventPublisher implements DomainEvents {
    private final CommerceOutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void publish(String eventType, String aggregateId, Map<String, Object> payload) {
        publish("cinx.domain.events", defaultRoutingKey(eventType), 1, eventType, aggregateId, payload, Map.of());
    }

    @Override
    public void publish(String exchange, String routingKey, int eventVersion, String eventType,
                        String aggregateId, Map<String, Object> payload, Map<String, Object> headers) {
        String eventId = UUID.randomUUID().toString();
        try {
            var body = new java.util.LinkedHashMap<>(payload);
            body.put("eventId", eventId);
            repository.save(CommerceOutbox.builder().eventId(eventId).eventType(eventType)
                    .aggregateId(aggregateId).payload(objectMapper.writeValueAsString(body))
                    .exchangeName(exchange).routingKey(routingKey).eventVersion(eventVersion)
                    .headersJson(objectMapper.writeValueAsString(headers))
                    .createdAt(LocalDateTime.now(clock)).build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize commerce event", exception);
        }
    }

    private String defaultRoutingKey(String eventType) {
        return switch (eventType) {
            case "CommerceOrderPaid" -> "commerce.order.paid";
            case "CommerceWishlistAdded" -> "commerce.wishlist.added";
            case "CommerceWishlistRemoved" -> "commerce.wishlist.removed";
            default -> "commerce.event";
        };
    }
}
