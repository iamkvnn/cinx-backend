package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.dispatch.application.NotificationMessage;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.user.FriendEventNotice;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/** Friend requests and accepts from user (contract J2): one in-app notice to the person concerned. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendNotificationListener {

    private final INotificationDispatchService dispatchService;
    private final IdempotencyService idempotencyService;
    private final NotificationFormatter notificationFormatter;

    @RabbitListener(queues = "notification.friend.queue", ackMode = "MANUAL")
    public void handle(FriendEventNotice event, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                       @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId,
                       @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey) {
        log.info("Received {} for requestId={}", routingKey, event.getRequestId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            NotificationContext ctx = context(routingKey, event);
            if (ctx == null) {
                log.warn("Ignoring friend notification routingKey={} requestId={} without a recipient or of an unknown kind",
                        routingKey, event.getRequestId());
                channel.basicAck(tag, false);
                return;
            }
            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing friend notification", e);
            nack(channel, tag);
        }
    }

    /** Null for an unknown routing key or a missing recipient. */
    private NotificationContext context(String routingKey, FriendEventNotice e) {
        return switch (routingKey == null ? "" : routingKey) {
            case "user.friend.requested" -> inApp(notificationFormatter.friendRequested(
                    e.getRequestId(), e.getFromUserId(), e.getFromName()), e.getToUserId());
            case "user.friend.accepted" -> inApp(notificationFormatter.friendAccepted(
                    e.getRequestId(), e.getAccepterId(), e.getAccepterName()), e.getRequesterId());
            default -> null;
        };
    }

    private NotificationContext inApp(NotificationMessage notification, String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return null;
        }
        return NotificationContext.builder()
                .channels(List.of("IN_APP"))
                .inAppPayload(notification.inAppPayload(List.of(recipient)))
                .build();
    }

    private void nack(Channel channel, long tag) {
        try {
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            log.error("Error nacking message", ex);
        }
    }
}
