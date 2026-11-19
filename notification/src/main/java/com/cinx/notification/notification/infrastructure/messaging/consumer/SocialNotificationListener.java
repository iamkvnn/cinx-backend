package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.common.dto.ApiResponse;
import com.cinx.notification.notification.infrastructure.client.CourseClient;
import com.cinx.notification.notification.infrastructure.client.UserClient;
import com.cinx.notification.notification.infrastructure.client.dto.course.CourseResponse;
import com.cinx.notification.notification.infrastructure.client.dto.user.UserDto;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.social.CourseAnswerCreatedEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.social.CourseQuestionCreatedEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.social.CourseReviewCreatedEvent;
import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.dispatch.application.NotificationMessage;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.rabbitmq.client.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "notification.social.queue", ackMode = "MANUAL")
public class SocialNotificationListener {

    private final CourseClient courseClient;
    private final UserClient userClient;
    private final INotificationDispatchService dispatchService;
    private final IdempotencyService idempotencyService;
    private final NotificationFormatter notificationFormatter;
    private final ObjectMapper objectMapper;

    @RabbitHandler(isDefault = true)
    public void handleGenericSocialEvent(Map<String, Object> payload, Channel channel,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                         @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId,
                                         @Header(value = "eventType", required = false) String eventType) {
        String resolvedEventType = eventType != null ? eventType : inferEventType(payload);
        if (resolvedEventType == null) {
            log.warn("Ignoring unsupported social notification eventType={} payload={}", eventType, payload);
            ack(channel, tag);
            return;
        }
        switch (resolvedEventType) {
            case "CourseReviewCreated" -> handleReviewCreated(
                    objectMapper.convertValue(payload, CourseReviewCreatedEvent.class), channel, tag, messageId);
            case "CourseQuestionCreated" -> handleQuestionCreated(
                    objectMapper.convertValue(payload, CourseQuestionCreatedEvent.class), channel, tag, messageId);
            case "CourseAnswerCreated" -> handleAnswerCreated(
                    objectMapper.convertValue(payload, CourseAnswerCreatedEvent.class), channel, tag, messageId);
            default -> {
                log.warn("Ignoring unsupported social notification eventType={} payload={}", eventType, payload);
                ack(channel, tag);
            }
        }
    }

    @RabbitHandler
    public void handleReviewCreated(CourseReviewCreatedEvent event, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                    @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received review created event: {}", event.getReviewId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }

            ApiResponse<CourseResponse> courseRes = courseClient.getCourseById(event.getCourseId());
            if (courseRes != null && courseRes.success() && courseRes.data() != null) {
                CourseResponse course = courseRes.data();
                String fallbackInstructorId = course.instructor() == null ? null : course.instructor().id();
                List<String> instructorIds = resolveInstructorIds(event.getCourseId(), fallbackInstructorId).stream()
                        .filter(id -> !id.equals(event.getReviewerUserId()))
                        .toList();
                if (!instructorIds.isEmpty()) {
                    String reviewerName = resolveUserName(event.getReviewerUserId());
                    NotificationMessage notification = notificationFormatter.courseReviewCreated(
                            event.getCourseId(), course.title(), reviewerName, event.getRating());

                    NotificationContext ctx = NotificationContext.builder()
                            .channels(List.of("IN_APP"))
                            .inAppPayload(notification.inAppPayload(instructorIds))
                            .build();
                    dispatchService.dispatch(ctx);
                }
            }

            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing review created event", e);
            nack(channel, tag);
        }
    }

    @RabbitHandler
    public void handleQuestionCreated(CourseQuestionCreatedEvent event, Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                      @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received question created event: {}", event.getQuestionId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }

            ApiResponse<CourseResponse> courseRes = courseClient.getCourseById(event.getCourseId());
            if (courseRes != null && courseRes.success() && courseRes.data() != null) {
                CourseResponse course = courseRes.data();
                String fallbackInstructorId = course.instructor() == null ? null : course.instructor().id();
                List<String> instructorIds = resolveInstructorIds(event.getCourseId(), fallbackInstructorId).stream()
                        .filter(id -> !id.equals(event.getAskedByUserId()))
                        .toList();
                if (!instructorIds.isEmpty()) {
                        NotificationMessage notification = notificationFormatter.courseQuestionCreated(
                                event.getCourseId(), course.title(), event.getQuestionId(), event.getQuestionTitle());
                        NotificationContext ctx = NotificationContext.builder()
                                .channels(List.of("IN_APP"))
                                .inAppPayload(notification.inAppPayload(instructorIds))
                                .build();
                        dispatchService.dispatch(ctx);
                }
            }
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing question created event", e);
            nack(channel, tag);
        }
    }

    @RabbitHandler
    public void handleAnswerCreated(CourseAnswerCreatedEvent event, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                    @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received answer created event: {}", event.getAnswerId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }

            String targetUserId;
            boolean replyToAnswer;

            if (event.getParentAnswerAuthorId() != null) {
                targetUserId = event.getParentAnswerAuthorId();
                replyToAnswer = true;
            } else if (event.getQuestionAuthorId() != null) {
                targetUserId = event.getQuestionAuthorId();
                replyToAnswer = false;
            } else {
                idempotencyService.markSuccess(messageId);
                channel.basicAck(tag, false);
                return;
            }

            if (!targetUserId.equals(event.getAnsweredByUserId())) {
                NotificationMessage notification = notificationFormatter.courseAnswerCreated(
                        event.getCourseId(), event.getQuestionId(), event.getAnswerId(), replyToAnswer);
                NotificationContext ctx = NotificationContext.builder()
                        .channels(List.of("IN_APP"))
                        .inAppPayload(notification.inAppPayload(List.of(targetUserId)))
                        .build();
                dispatchService.dispatch(ctx);
            }
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing answer created event", e);
            nack(channel, tag);
        }
    }

    private void nack(Channel channel, long tag) {
        try {
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            log.error("Error nacking message", ex);
        }
    }

    private void ack(Channel channel, long tag) {
        try {
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.error("Error acking message", ex);
        }
    }

    private String inferEventType(Map<String, Object> payload) {
        if (payload.containsKey("reviewId")) {
            return "CourseReviewCreated";
        }
        if (payload.containsKey("questionTitle")) {
            return "CourseQuestionCreated";
        }
        if (payload.containsKey("answerId")) {
            return "CourseAnswerCreated";
        }
        return null;
    }

    private String resolveUserName(String userId) {
        try {
            ApiResponse<UserDto> userRes = userClient.getUserById(userId);
            if (userRes != null && userRes.success() && userRes.data() != null
                    && userRes.data().name() != null && !userRes.data().name().isBlank()) {
                return userRes.data().name();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve reviewer name for userId={}: {}", userId, ex.getMessage());
        }
        return "A student";
    }

    private List<String> resolveInstructorIds(String courseId, String fallbackInstructorId) {
        try {
            ApiResponse<List<String>> response = courseClient.getInstructorIds(courseId);
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
            log.warn("Could not resolve active instructors for courseId={}: {}", courseId, ex.getMessage());
        }
        return fallbackInstructorId == null || fallbackInstructorId.isBlank()
                ? List.of()
                : List.of(fallbackInstructorId);
    }
}
