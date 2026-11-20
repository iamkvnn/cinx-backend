package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthNotificationListener {

    private final INotificationDispatchService dispatchService;
    private final IdempotencyService idempotencyService;

    @RabbitListener(queues = "notification.auth.queue", ackMode = "MANUAL",
            containerFactory = "rabbitListenerContainerFactory")
    public void handleAuthEvent(Map<String, Object> payload, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
                                @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received auth event, routingKey={}", routingKey);
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            if (!isEmailPayload(payload)) {
                log.debug("Ignoring auth event without an email delivery contract, routingKey={}", routingKey);
                idempotencyService.markSuccess(messageId);
                channel.basicAck(tag, false);
                return;
            }
            NotificationContext ctx = NotificationContext.builder()
                    .channels(List.of("EMAIL"))
                    .emailPayload(payload)
                    .build();
            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing auth event, routingKey={}", routingKey, e);
            nack(channel, tag);
        }
    }

    private boolean isEmailPayload(Map<String, Object> payload) {
        return payload.get("to") != null && payload.get("subject") != null && payload.get("body") != null;
    }

    private void nack(Channel channel, long tag) {
        try {
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            log.error("Error nacking message", ex);
        }
    }
}
