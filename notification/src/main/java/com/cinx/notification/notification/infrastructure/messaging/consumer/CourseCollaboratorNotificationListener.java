package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.common.dto.ApiResponse;
import com.cinx.notification.notification.infrastructure.client.UserClient;
import com.cinx.notification.notification.infrastructure.client.dto.user.UserDto;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.course.CourseCollaboratorInvitedEvent;
import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.dispatch.application.NotificationMessage;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Tells an instructor they were invited to co-teach a course: in-app, plus email when the address is known. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseCollaboratorNotificationListener {

    private final UserClient userClient;
    private final INotificationDispatchService dispatchService;
    private final IdempotencyService idempotencyService;
    private final NotificationFormatter notificationFormatter;

    @RabbitListener(queues = "notification.course-collaborator.queue", ackMode = "MANUAL")
    public void handleCollaboratorInvited(CourseCollaboratorInvitedEvent event, Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                          @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received course collaborator invited event for courseId: {}", event.getCourseId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            if (event.getInviteeId() == null || event.getInviteeId().isBlank()) {
                throw new IllegalStateException("Collaborator invited event missing inviteeId");
            }
            UserDto invitee = user(event.getInviteeId());
            UserDto inviter = user(event.getInviterId());
            NotificationMessage notification = notificationFormatter.courseCollaboratorInvited(
                    event.getCourseId(), event.getCourseTitle(), inviter == null ? null : inviter.name(), roleLabel(event.getRole()));

            List<String> channels = new ArrayList<>(List.of("IN_APP"));
            NotificationContext.Builder ctx = NotificationContext.builder()
                    .inAppPayload(notification.inAppPayload(List.of(event.getInviteeId())));
            if (invitee != null && invitee.email() != null && !invitee.email().isBlank()) {
                channels.add("EMAIL");
                ctx.emailPayload(notification.emailPayload(invitee.email()));
            }
            dispatchService.dispatch(ctx.channels(channels).build());

            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing course collaborator invited event", e);
            nack(channel, tag);
        }
    }

    private static String roleLabel(String role) {
        if ("MANAGER".equals(role)) {
            return "Quản lý";
        }
        if ("EDITOR".equals(role)) {
            return "Biên tập";
        }
        return null;
    }

    private UserDto user(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            ApiResponse<UserDto> response = userClient.getUserById(userId);
            return response != null && response.success() ? response.data() : null;
        } catch (Exception e) {
            log.warn("Could not resolve user for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    private void nack(Channel channel, long tag) {
        try {
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            log.error("Error nacking message", ex);
        }
    }
}
