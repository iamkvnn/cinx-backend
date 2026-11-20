package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.common.dto.ApiResponse;
import com.cinx.notification.notification.infrastructure.client.IdentityClient;
import com.cinx.notification.notification.infrastructure.client.UserClient;
import com.cinx.notification.notification.infrastructure.client.dto.user.UserDto;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.course.CourseApprovalRequestedEvent;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.dispatch.application.NotificationMessage;
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
public class CourseApprovalNotificationListener {

    private final UserClient userClient;
    private final IdentityClient identityClient;
    private final INotificationDispatchService dispatchService;
    private final IdempotencyService idempotencyService;
    private final NotificationFormatter notificationFormatter;

    @RabbitListener(queues = "notification.course-approval.queue", ackMode = "MANUAL")
    public void handleCourseApprovalRequested(CourseApprovalRequestedEvent event, Channel channel,
                                              @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                              @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received course approval requested event for courseId: {}", event.getCourseId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }

            ApiResponse<List<String>> adminIdsResponse = identityClient.getCourseReviewerUserIds();
            if (adminIdsResponse == null || !adminIdsResponse.success()
                    || adminIdsResponse.data() == null || adminIdsResponse.data().isEmpty()) {
                log.warn("No admin recipients found for course approval notification, courseId={}", event.getCourseId());
                idempotencyService.markSuccess(messageId);
                channel.basicAck(tag, false);
                return;
            }

            String courseTitle = event.getCourseTitle() == null || event.getCourseTitle().isBlank()
                    ? "A course"
                    : event.getCourseTitle();
            String instructorName = resolveUserName(event.getInstructorId());
            NotificationMessage notification = notificationFormatter.courseApprovalRequested(
                    event.getCourseId(), courseTitle, instructorName);

            NotificationContext ctx = NotificationContext.builder()
                    .channels(List.of("IN_APP"))
                    .inAppPayload(notification.inAppPayload(adminIdsResponse.data()))
                    .build();
            dispatchService.dispatch(ctx);

            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing course approval requested event", e);
            nack(channel, tag);
        }
    }

    private String resolveUserName(String userId) {
        if (userId == null || userId.isBlank()) {
            return "An instructor";
        }
        try {
            ApiResponse<UserDto> userResponse = userClient.getUserById(userId);
            if (userResponse != null && userResponse.success() && userResponse.data() != null
                    && userResponse.data().name() != null && !userResponse.data().name().isBlank()) {
                return userResponse.data().name();
            }
        } catch (Exception e) {
            log.warn("Could not resolve instructor name for userId={}: {}", userId, e.getMessage());
        }
        return "An instructor";
    }

    private void nack(Channel channel, long tag) {
        try {
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            log.error("Error nacking message", ex);
        }
    }
}
