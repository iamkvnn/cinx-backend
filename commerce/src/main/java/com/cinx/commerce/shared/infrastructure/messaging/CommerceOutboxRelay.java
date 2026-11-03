package com.cinx.commerce.shared.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component @Slf4j
public class CommerceOutboxRelay {
    private final CommerceOutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean confirmationRequired;
    @Value("${cinx.entitlement.publication-enabled:false}")
    private boolean entitlementPublicationEnabled;

    @Autowired
    public CommerceOutboxRelay(CommerceOutboxRepository repository, RabbitTemplate rabbitTemplate,
                               ObjectMapper objectMapper, Clock clock) {
        this(repository, rabbitTemplate, objectMapper, clock, true);
    }

    /** Compatibility constructor used by focused unit tests and local fakes. */
    public CommerceOutboxRelay(CommerceOutboxRepository repository, RabbitTemplate rabbitTemplate) {
        this(repository, rabbitTemplate, new ObjectMapper(), Clock.systemUTC(), false);
    }

    private CommerceOutboxRelay(CommerceOutboxRepository repository, RabbitTemplate rabbitTemplate,
                                ObjectMapper objectMapper, Clock clock, boolean confirmationRequired) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.confirmationRequired = confirmationRequired;
    }

    @Scheduled(fixedDelayString = "${commerce.outbox.fixed-delay-ms:1000}")
    @Transactional
    public void relay() {
        for (CommerceOutbox event : repository.lockPendingBatch()) {
            if ("commerce.subscription.entitlements.changed".equals(event.getEventType())
                    && !entitlementPublicationEnabled) {
                // Keep the committed outbox row pending while the consumer-first
                // rollout switch is disabled. Enabling the switch later allows
                // the same durable row to be published without rebuilding state.
                continue;
            }
            try {
                String exchange = event.getExchangeName() == null ? "cinx.domain.events" : event.getExchangeName();
                String routingKey = event.getRoutingKey() == null ? routingKey(event.getEventType()) : event.getRoutingKey();
                var messageBuilder = MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                                .setContentType("application/json").setMessageId(event.getEventId())
                                .setHeader("eventId", event.getEventId())
                                .setHeader("eventType", event.getEventType())
                                .setHeader("eventVersion", event.getEventVersion())
                                .setHeader("aggregateType", aggregateType(event.getEventType()))
                                .setHeader("aggregateId", event.getAggregateId())
                                .setHeader("occurredAt", event.getCreatedAt().atOffset(ZoneOffset.UTC).toInstant().toString());
                if (event.getHeadersJson() != null && !event.getHeadersJson().isBlank()) {
                    try {
                        objectMapper.readValue(event.getHeadersJson(), new TypeReference<java.util.Map<String, Object>>() {})
                                .forEach(messageBuilder::setHeader);
                    } catch (Exception exception) {
                        throw new IllegalStateException("Invalid outbox headers", exception);
                    }
                }
                var message = messageBuilder.build();
                if (confirmationRequired) {
                    Boolean confirmed = rabbitTemplate.invoke(operations -> {
                        operations.send(exchange, routingKey, message);
                        return operations.waitForConfirms(5000);
                    }, null, null);
                    if (!Boolean.TRUE.equals(confirmed)) {
                        throw new IllegalStateException("RabbitMQ publisher confirmation was negative or timed out");
                    }
                } else {
                    rabbitTemplate.send(exchange, routingKey, message);
                }
                event.setStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now(clock));
                event.setLastError(null);
            } catch (RuntimeException exception) {
                event.setAttempts(event.getAttempts() + 1);
                event.setStatus(event.getAttempts() >= 10 ? "FAILED" : "PENDING");
                event.setNextAttemptAt(LocalDateTime.now(clock).plusSeconds(Math.min(300, 1L << Math.min(8, event.getAttempts()))));
                event.setLastError(exception.getMessage());
                log.error("Commerce outbox publish failed eventId={}", event.getEventId(), exception);
            }
        }
    }

    private String routingKey(String eventType) {
        return switch (eventType) {
            case "CommerceOrderPaid" -> "commerce.order.paid";
            case "CommerceWishlistAdded" -> "commerce.wishlist.added";
            case "CommerceWishlistRemoved" -> "commerce.wishlist.removed";
            default -> "commerce.event";
        };
    }

    private String aggregateType(String eventType) {
        return switch (eventType) {
            case "CommerceOrderPaid" -> "CommerceOrder";
            case "CommerceWishlistAdded", "CommerceWishlistRemoved" -> "WishlistItem";
            case "commerce.subscription.entitlements.changed" -> "SubjectEntitlement";
            default -> "Commerce";
        };
    }
}
