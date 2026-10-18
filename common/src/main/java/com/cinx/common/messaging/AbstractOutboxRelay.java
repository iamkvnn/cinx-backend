package com.cinx.common.messaging;

import com.cinx.common.logging.CorrelationContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractOutboxRelay<T extends OutboxMessageBase> {
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxRepository<T> outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    protected AbstractOutboxRelay(OutboxRepository<T> outboxRepository,
                                  RabbitTemplate rabbitTemplate,
                                  ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publishPending() {
        var messages = outboxRepository.lockPendingBatch();
        messages.forEach(this::publishOne);
    }

    private void publishOne(T outboxMessage) {
        outboxMessage.setStatus(OutboxStatus.PUBLISHING);
        outboxMessage.setAttempts(outboxMessage.getAttempts() + 1);
        outboxRepository.saveAndFlush(outboxMessage);
        try {
            Message message = toAmqpMessage(outboxMessage);
            CorrelationData correlationData = new CorrelationData(outboxMessage.getEventId());
            rabbitTemplate.send(outboxMessage.getExchangeName(), outboxMessage.getRoutingKey(), message, correlationData);
            var confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("Broker nack: " + confirm.getReason());
            }
            outboxMessage.setStatus(OutboxStatus.PUBLISHED);
            outboxMessage.setPublishedAt(LocalDateTime.now());
            outboxMessage.setLastError(null);
        } catch (Exception e) {
            log.error("Failed to publish outbox eventId={}", outboxMessage.getEventId(), e);
            outboxMessage.setLastError(e.getMessage());
            if (outboxMessage.getAttempts() >= MAX_ATTEMPTS) {
                outboxMessage.setStatus(OutboxStatus.FAILED);
            } else {
                outboxMessage.setStatus(OutboxStatus.PENDING);
                outboxMessage.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds(outboxMessage.getAttempts())));
            }
        }
        outboxMessage.setUpdatedAt(LocalDateTime.now());
        outboxRepository.save(outboxMessage);
    }

    private Message toAmqpMessage(T outboxMessage) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setMessageId(outboxMessage.getEventId());
        properties.setHeader("eventId", outboxMessage.getEventId());
        properties.setHeader("eventType", outboxMessage.getEventType());
        properties.setHeader("eventVersion", outboxMessage.getEventVersion());
        properties.setHeader("aggregateType", outboxMessage.getAggregateType());
        properties.setHeader("aggregateId", outboxMessage.getAggregateId());
        properties.setHeader("occurredAt", outboxMessage.getCreatedAt().toString());
        if (outboxMessage.getHeadersJson() != null && !outboxMessage.getHeadersJson().isBlank()) {
            Map<String, Object> headers = objectMapper.readValue(
                    outboxMessage.getHeadersJson(),
                    new TypeReference<>() {}
            );
            headers.forEach(properties::setHeader);
        }
        CorrelationContext.applyMissingToMessageProperties(properties);
        return new Message(outboxMessage.getPayloadJson().getBytes(StandardCharsets.UTF_8), properties);
    }

    private long backoffSeconds(int attempts) {
        return Math.min(300, (long) Math.pow(2, Math.max(0, attempts - 1)));
    }
}
