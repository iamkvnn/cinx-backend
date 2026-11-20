package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.dispatch.application.NotificationMessage;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.campus.CampusEventNotice;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Campus events for learners: a new event in an instructor's zone, a reminder 15 minutes before one they rang the bell
 * on, its cancellation, and a reply to their thread on a zone's or club's board. The campus already chose the
 * recipients; one message carries at most 500 of them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CampusNotificationListener {

    private final INotificationDispatchService dispatchService;
    private final IdempotencyService idempotencyService;
    private final NotificationFormatter notificationFormatter;

    @RabbitListener(queues = "notification.campus.queue", ackMode = "MANUAL")
    public void handle(CampusEventNotice event, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                       @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId,
                       @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey) {
        log.info("Received {} for eventId={}, recipients={}", routingKey, event.getEventId(),
                event.getRecipientIds() == null ? 0 : event.getRecipientIds().size());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            List<String> recipients = event.getRecipientIds() == null ? List.of()
                    : event.getRecipientIds().stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
            NotificationContext ctx = recipients.isEmpty() ? null : context(routingKey, event, recipients);
            if (ctx == null) {
                log.warn("Ignoring campus notification routingKey={} eventId={} without recipients or of an unknown kind",
                        routingKey, event.getEventId());
                channel.basicAck(tag, false);
                return;
            }
            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing campus event notification", e);
            nack(channel, tag);
        }
    }

    /**
     * The new-event notice goes to many learners, like new course content: in the app only. The reminder and the
     * cancellation answer the learner's own bell, like a study reminder: also pushed. A reply on the learner's board
     * thread stays in the app. Null for an unknown routing key.
     */
    private NotificationContext context(String routingKey, CampusEventNotice e, List<String> recipients) {
        return switch (routingKey == null ? "" : routingKey) {
            case "campus.event.scheduled" -> inApp(notificationFormatter.campusEventScheduled(
                    e.getEventId(), e.getZoneId(), e.getZoneTitle(), e.getTitle(), e.getKind(), e.getStartsAt()), recipients);
            case "campus.event.reminder.due" -> inAppAndPush(notificationFormatter.campusEventReminder(
                    e.getEventId(), e.getZoneId(), e.getZoneTitle(), e.getTitle(), e.getKind(), e.getStartsAt()), recipients);
            case "campus.event.cancelled" -> inAppAndPush(notificationFormatter.campusEventCancelled(
                    e.getEventId(), e.getZoneId(), e.getZoneTitle(), e.getTitle(), e.getStartsAt()), recipients);
            case "campus.thread.replied" -> inApp(notificationFormatter.campusThreadReply(
                    e.getThreadId(), e.getReplyId(), e.getPlaceKind(), e.getPlaceId(), e.getPlaceTitle(), e.getTitle(),
                    e.getReplierName()), recipients);
            default -> null;
        };
    }

    private NotificationContext inApp(NotificationMessage notification, List<String> recipients) {
        return NotificationContext.builder()
                .channels(List.of("IN_APP"))
                .inAppPayload(notification.inAppPayload(recipients))
                .build();
    }

    private NotificationContext inAppAndPush(NotificationMessage notification, List<String> recipients) {
        return NotificationContext.builder()
                .channels(List.of("IN_APP", "PUSH"))
                .inAppPayload(notification.inAppPayload(recipients))
                .pushPayload(notification.pushPayload(recipients))
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
