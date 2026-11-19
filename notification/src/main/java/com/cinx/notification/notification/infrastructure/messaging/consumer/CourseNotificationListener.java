package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.common.dto.ApiResponse;
import com.cinx.notification.notification.infrastructure.client.CourseClient;
import com.cinx.notification.notification.infrastructure.client.EnrollmentClient;
import com.cinx.notification.notification.infrastructure.client.UserClient;
import com.cinx.notification.notification.infrastructure.client.dto.user.UserDto;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.course.CourseContentPublishedEvent;
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
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseNotificationListener {

    private final EnrollmentClient enrollmentClient;
    private final CourseClient courseClient;
    private final UserClient userClient;
    private final INotificationDispatchService dispatchService;
    private final IdempotencyService idempotencyService;
    private final NotificationFormatter notificationFormatter;

    @RabbitListener(queues = "notification.course.queue", ackMode = "MANUAL")
    public void handleCourseContentPublished(CourseContentPublishedEvent event, Channel channel,
                                             @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                             @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received course content published event for courseId: {}", event.getCourseId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            String courseTitle = event.getCourseTitle();
            if (courseTitle == null || courseTitle.isBlank()) {
                courseTitle = "your course";
            }

            notifyInstructorCoursePublished(event, courseTitle);

            ApiResponse<List<String>> enrolledRes = enrollmentClient.getUserIdsEnrolledInCourse(event.getCourseId());
            if (enrolledRes == null || !enrolledRes.success()
                    || enrolledRes.data() == null || enrolledRes.data().isEmpty()) {
                log.info("No enrolled users for courseId: {}", event.getCourseId());
                channel.basicAck(tag, false);
                return;
            }

            List<String> userIds = enrolledRes.data();

            NotificationMessage notification = notificationFormatter.coursePublishedForLearners(
                    event.getCourseId(), courseTitle);

            // In-app: one batch for all enrolled users
            NotificationContext inAppCtx = NotificationContext.builder()
                    .channels(List.of("IN_APP"))
                    .inAppPayload(notification.inAppPayload(userIds))
                    .build();
            dispatchService.dispatch(inAppCtx);

            // Email: one per user (requires their email — still needs Feign)
            for (String userId : userIds) {
                try {
                    ApiResponse<UserDto> userRes = userClient.getUserById(userId);
                    if (userRes != null && userRes.success() && userRes.data() != null) {
                        String email = userRes.data().email();
                        if (email != null && !email.isBlank()) {
                            NotificationContext emailCtx = NotificationContext.builder()
                                    .channels(List.of("EMAIL"))
                                    .emailPayload(notification.emailPayload(email))
                                    .build();
                            dispatchService.dispatch(emailCtx);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Could not send email for userId={}: {}", userId, ex.getMessage());
                }
            }

            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing course content published event: ", e);
            try {
                channel.basicNack(tag, false, false);
            } catch (Exception ex) {
                log.error("Error nacking message", ex);
            }
        }
    }

    private void notifyInstructorCoursePublished(CourseContentPublishedEvent event, String courseTitle) {
        List<String> instructorIds = resolveInstructorIds(event);
        if (instructorIds.isEmpty()) {
            return;
        }

        List<String> notifiedIds = new java.util.ArrayList<>();
        for (String instructorId : instructorIds) {
            try {
                ApiResponse<UserDto> userRes = userClient.getUserById(instructorId);
                if (userRes == null || !userRes.success() || userRes.data() == null) {
                    log.warn("Failed to fetch instructor details for userId={}", instructorId);
                    continue;
                }
                UserDto instructor = userRes.data();
                notifiedIds.add(instructorId);
                NotificationMessage notification = notificationFormatter.coursePublishedForInstructor(
                        event.getCourseId(), courseTitle, instructor.name());
                if (instructor.email() != null && !instructor.email().isBlank()) {
                    NotificationContext emailContext = NotificationContext.builder()
                            .channels(List.of("EMAIL"))
                            .emailPayload(notification.emailPayload(instructor.email()))
                            .build();
                    dispatchService.dispatch(emailContext);
                }
            } catch (Exception ex) {
                log.warn("Could not notify instructor userId={}: {}", instructorId, ex.getMessage());
            }
        }

        if (!notifiedIds.isEmpty()) {
            NotificationMessage notification = notificationFormatter.coursePublishedForInstructor(
                    event.getCourseId(), courseTitle, "your instructor team");
            NotificationContext inAppContext = NotificationContext.builder()
                    .channels(List.of("IN_APP"))
                    .inAppPayload(notification.inAppPayload(notifiedIds))
                    .build();
            dispatchService.dispatch(inAppContext);
        }
    }

    private List<String> resolveInstructorIds(CourseContentPublishedEvent event) {
        if (event.getInstructorIds() != null && !event.getInstructorIds().isEmpty()) {
            List<String> snapshot = event.getInstructorIds().stream()
                    .filter(Objects::nonNull)
                    .filter(id -> !id.isBlank())
                    .distinct()
                    .toList();
            if (!snapshot.isEmpty()) {
                return snapshot;
            }
        }
        try {
            ApiResponse<List<String>> response = courseClient.getInstructorIds(event.getCourseId());
            if (response != null && response.success() && response.data() != null) {
                List<String> ids = response.data().stream()
                        .filter(Objects::nonNull)
                        .filter(id -> !id.isBlank())
                        .distinct()
                        .toList();
                if (!ids.isEmpty()) {
                    return ids;
                }
            }
        } catch (Exception ex) {
            log.warn("Could not resolve active instructors for courseId={}: {}",
                    event.getCourseId(), ex.getMessage());
        }
        if (event.getInstructorId() == null || event.getInstructorId().isBlank()) {
            return List.of();
        }
        return List.of(event.getInstructorId());
    }
}
