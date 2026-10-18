package com.cinx.common.messaging;

import com.cinx.common.logging.CorrelationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public abstract class AbstractOutboxEventPublisher<T extends OutboxMessageBase> {
    private final OutboxRepository<T> outboxRepository;
    private final ObjectMapper objectMapper;

    protected AbstractOutboxEventPublisher(OutboxRepository<T> outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    protected abstract T newMessage();

    @Transactional
    public void enqueue(String eventId,
                        String aggregateType,
                        String aggregateId,
                        String eventType,
                        String exchange,
                        String routingKey,
                        Object payload) {
        enqueue(eventId, aggregateType, aggregateId, eventType, exchange, routingKey, payload, Map.of());
    }

    @Transactional
    public void enqueue(String eventId,
                        String aggregateType,
                        String aggregateId,
                        String eventType,
                        String exchange,
                        String routingKey,
                        Object payload,
                        Map<String, Object> headers) {
        enqueue(eventId, aggregateType, aggregateId, eventType, 1, exchange, routingKey, payload, headers);
    }

    @Transactional
    public void enqueue(String eventId,
                        String aggregateType,
                        String aggregateId,
                        String eventType,
                        int eventVersion,
                        String exchange,
                        String routingKey,
                        Object payload,
                        Map<String, Object> headers) {
        if (outboxRepository.existsByEventId(eventId)) {
            return;
        }
        T message = newMessage();
        message.setEventId(eventId);
        message.setAggregateType(aggregateType);
        message.setAggregateId(aggregateId);
        message.setEventType(eventType);
        message.setEventVersion(eventVersion);
        message.setExchangeName(exchange);
        message.setRoutingKey(routingKey);
        message.setStatus(OutboxStatus.PENDING);
        message.setAttempts(0);
        Map<String, Object> eventHeaders = new java.util.LinkedHashMap<>(headers);
        eventHeaders.putIfAbsent(CorrelationContext.TRACEPARENT_HEADER, CorrelationContext.currentTraceparent());
        String requestId = CorrelationContext.currentRequestId();
        if (requestId != null && !requestId.isBlank()) {
            eventHeaders.putIfAbsent(CorrelationContext.REQUEST_ID_HEADER, requestId);
        }
        try {
            message.setPayloadJson(objectMapper.writeValueAsString(payload));
            message.setHeadersJson(objectMapper.writeValueAsString(eventHeaders));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
        outboxRepository.save(message);
    }
}
