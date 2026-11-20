package com.cinx.commerce.shared.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommerceOutboxRelayTest {
    @Test
    void relayPublishesRequiredDomainEventHeaders() {
        CommerceOutboxRepository repository = mock(CommerceOutboxRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        CommerceOutbox event = CommerceOutbox.builder()
                .eventId("event-1")
                .eventType("CommerceOrderPaid")
                .aggregateId("order-1")
                .payload("{}")
                .createdAt(LocalDateTime.parse("2026-07-11T12:00:00"))
                .build();
        when(repository.lockPendingBatch()).thenReturn(List.of(event));

        new CommerceOutboxRelay(repository, rabbitTemplate).relay();

        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(org.mockito.ArgumentMatchers.eq("cinx.domain.events"),
                org.mockito.ArgumentMatchers.eq("commerce.order.paid"), message.capture());
        var headers = message.getValue().getMessageProperties().getHeaders();
        assertEquals("event-1", headers.get("eventId"));
        assertEquals("CommerceOrderPaid", headers.get("eventType"));
        assertEquals(1, headers.get("eventVersion"));
        assertEquals("CommerceOrder", headers.get("aggregateType"));
        assertEquals("order-1", headers.get("aggregateId"));
        assertEquals("2026-07-11T12:00:00Z", headers.get("occurredAt"));
    }
}
