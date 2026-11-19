package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.learning.CertificateApprovedEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.learning.CertificateRequestedEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.learning.CourseCompletedEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.learning.LabReviewDecidedEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.learning.LabSessionEndingEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.learning.LabSessionReadyEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.learning.DailyReminderDueEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.learning.StudyReminderDueEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.learning.SubtitleJobFinishedEvent;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.dispatch.application.NotificationMessage;
import com.cinx.notification.dispatch.application.INotificationDispatchService;
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

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "notification.learning.queue", ackMode = "MANUAL")
public class LearningNotificationListener {

    private final INotificationDispatchService dispatchService;
    private final IdempotencyService idempotencyService;
    private final NotificationFormatter notificationFormatter;
    private final ObjectMapper objectMapper;

    @RabbitHandler(isDefault = true)
    public void handleGenericLearningEvent(Map<String, Object> payload, Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                           @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId,
                                           @Header(value = "eventType", required = false) String eventType) {
        String resolvedEventType = eventType != null ? eventType : inferEventType(payload);
        if (resolvedEventType == null) {
            log.warn("Ignoring unsupported learning notification eventType={} payload={}", eventType, payload);
            ack(channel, tag);
            return;
        }
        switch (resolvedEventType) {
            case "learning.certificate.requested", "CertificateRequested" -> handleCertificateRequested(
                    objectMapper.convertValue(payload, CertificateRequestedEvent.class), channel, tag, messageId);
            case "learning.certificate.approved", "CertificateApproved" -> handleCertificateApproved(
                    objectMapper.convertValue(payload, CertificateApprovedEvent.class), channel, tag, messageId);
            case "learning.course.completed", "CourseCompleted" -> handleCourseCompleted(
                    objectMapper.convertValue(payload, CourseCompletedEvent.class), channel, tag, messageId);
            case "learning.reminder.due", "DailyReminderDue" -> handleDailyReminderDue(
                    objectMapper.convertValue(payload, DailyReminderDueEvent.class), channel, tag, messageId);
            case "learning.study-reminder.due" -> handleStudyReminderDue(
                    objectMapper.convertValue(payload, StudyReminderDueEvent.class), channel, tag, messageId);
            case "learning.subtitle.job.finished" -> handleSubtitleJobFinished(
                    objectMapper.convertValue(payload, SubtitleJobFinishedEvent.class), channel, tag, messageId);
            case "learning.lab.session.ready", "LabSessionReady" -> {
                LabSessionReadyEvent e = objectMapper.convertValue(payload, LabSessionReadyEvent.class);
                toLearner(e.getUserId(), notificationFormatter.labSessionReady(e.getSessionId(), e.getCourseId(), e.getLessonId(), e.getLessonTitle()),
                        channel, tag, messageId);
            }
            case "learning.lab.session.ending", "LabSessionEnding" -> {
                LabSessionEndingEvent e = objectMapper.convertValue(payload, LabSessionEndingEvent.class);
                toLearner(e.getUserId(), notificationFormatter.labSessionEnding(e.getSessionId(), e.getCourseId(), e.getLessonId(), e.getLessonTitle(),
                        e.getMinutesLeft()), channel, tag, messageId);
            }
            case "learning.lab.review.decided", "LabReviewDecided" -> {
                LabReviewDecidedEvent e = objectMapper.convertValue(payload, LabReviewDecidedEvent.class);
                toLearner(e.getInstructorId(), notificationFormatter.labReviewDecided(e.getReleaseId(), e.getCourseId(), e.getLessonId(), e.getLessonTitle(),
                        "APPROVED".equals(e.getDecision()), e.getReason()), channel, tag, messageId);
            }
            default -> {
                log.warn("Ignoring unsupported learning notification eventType={} payload={}", eventType, payload);
                ack(channel, tag);
            }
        }
    }

    @RabbitHandler
    public void handleCertificateRequested(CertificateRequestedEvent event, Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                           @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received certificate requested event for requestId={}, instructorId={}",
                event.getRequestId(), event.getInstructorId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            String courseTitle = event.getCourseTitle() != null ? event.getCourseTitle() : "your course";
            String studentName = event.getUserName() != null ? event.getUserName() : "A student";
            if (event.getInstructorId() == null || event.getInstructorId().isBlank()) {
                throw new IllegalStateException("Certificate request event missing instructorId");
            }
            if (event.getInstructorEmail() == null || event.getInstructorEmail().isBlank()) {
                throw new IllegalStateException("Certificate request event missing instructorEmail");
            }
            NotificationMessage notification = notificationFormatter.certificateRequested(
                    event.getRequestId(), studentName, event.getCourseId(), courseTitle);

            NotificationContext ctx = NotificationContext.builder()
                    .channels(List.of("EMAIL", "IN_APP"))
                    .emailPayload(notification.emailPayload(event.getInstructorEmail()))
                    .inAppPayload(notification.inAppPayload(List.of(event.getInstructorId())))
                    .build();

            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing certificate requested event", e);
            nack(channel, tag);
        }
    }

    @RabbitHandler
    public void handleCertificateApproved(CertificateApprovedEvent event, Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                          @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received certificate approved event for requestId={}, userId={}",
                event.getRequestId(), event.getUserId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            String courseTitle = event.getCourseTitle() != null ? event.getCourseTitle() : "your course";
            if (event.getUserId() == null || event.getUserId().isBlank()) {
                throw new IllegalStateException("Certificate approved event missing userId");
            }
            if (event.getUserEmail() == null || event.getUserEmail().isBlank()) {
                throw new IllegalStateException("Certificate approved event missing userEmail");
            }
            NotificationMessage notification = notificationFormatter.certificateApproved(
                    event.getRequestId(), event.getUserName(), event.getCourseId(), courseTitle, event.getCertificateUrl());

            NotificationContext ctx = NotificationContext.builder()
                    .channels(List.of("EMAIL", "IN_APP"))
                    .emailPayload(notification.emailPayload(event.getUserEmail()))
                    .inAppPayload(notification.inAppPayload(List.of(event.getUserId())))
                    .build();

            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing certificate approved event", e);
            nack(channel, tag);
        }
    }

    @RabbitHandler
    public void handleCourseCompleted(CourseCompletedEvent event, Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                      @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received course completed event for userId={}, courseId={}", event.getUserId(), event.getCourseId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            NotificationMessage notification = notificationFormatter.courseCompleted(
                    event.getCourseId(), event.getCourseTitle());

            NotificationContext ctx = NotificationContext.builder()
                    .channels(List.of("IN_APP", "PUSH"))
                    .inAppPayload(notification.inAppPayload(List.of(event.getUserId())))
                    .pushPayload(notification.pushPayload(List.of(event.getUserId())))
                    .build();

            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing course completed event", e);
            nack(channel, tag);
        }
    }

    @RabbitHandler
    public void handleDailyReminderDue(DailyReminderDueEvent event, Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                       @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received daily reminder event for userId={}", event.getUserId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            NotificationMessage notification = notificationFormatter.dailyReminder(
                    event.getGoalType(), event.getTargetValue(), event.getCurrentValue(), event.getTargetItemId());

            NotificationContext ctx = NotificationContext.builder()
                    .channels(List.of("IN_APP", "PUSH"))
                    .inAppPayload(notification.inAppPayload(List.of(event.getUserId())))
                    .pushPayload(notification.pushPayload(List.of(event.getUserId())))
                    .build();

            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing daily reminder event", e);
            nack(channel, tag);
        }
    }

    @RabbitHandler
    public void handleStudyReminderDue(StudyReminderDueEvent event, Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                       @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received study reminder for userId={}, reason={}", event.getUserId(), event.getReason());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            NotificationMessage notification = notificationFormatter.studyReminder(
                    event.getReason(), event.getTitle(), event.getBody(), event.getActionUrl(), event.getWrittenBy());
            NotificationContext ctx = NotificationContext.builder()
                    .channels(List.of("IN_APP", "PUSH"))
                    .inAppPayload(notification.inAppPayload(List.of(event.getUserId())))
                    .pushPayload(notification.pushPayload(List.of(event.getUserId())))
                    .build();
            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing study reminder event", e);
            nack(channel, tag);
        }
    }

    @RabbitHandler
    public void handleSubtitleJobFinished(SubtitleJobFinishedEvent event, Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                          @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received subtitle job finished for jobId={}, outcome={}", event.getJobId(), event.getOutcome());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            if (event.getUserId() == null || event.getUserId().isBlank()) {
                throw new IllegalStateException("Subtitle job event missing userId");
            }
            NotificationMessage notification = notificationFormatter.subtitleJobFinished(event.getJobId(), event.getOutcome(),
                    event.getDisplayName(), event.getLessonTitle(), event.getWarningCount(), event.getActionUrl());
            NotificationContext ctx = NotificationContext.builder()
                    .channels(List.of("IN_APP"))
                    .inAppPayload(notification.inAppPayload(List.of(event.getUserId())))
                    .build();
            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing subtitle job finished event", e);
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

    /** A lab notification to one user (the learner, or the instructor of a reviewed release), in the app and as a push */
    private void toLearner(String userId, NotificationMessage notification, Channel channel, long tag, String messageId) {
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            if (userId == null || userId.isBlank()) {
                throw new IllegalStateException("Lab notification event missing its user");
            }
            NotificationContext ctx = NotificationContext.builder()
                    .channels(List.of("IN_APP", "PUSH"))
                    .inAppPayload(notification.inAppPayload(List.of(userId)))
                    .pushPayload(notification.pushPayload(List.of(userId)))
                    .build();
            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing lab notification {}", notification.type(), e);
            nack(channel, tag);
        }
    }

    private String inferEventType(Map<String, Object> payload) {
        if (payload.containsKey("instructorId")) {
            return "learning.certificate.requested";
        }
        if (payload.containsKey("certificateUrl") || payload.containsKey("userEmail")) {
            return "learning.certificate.approved";
        }
        if (payload.containsKey("goalType")) {
            return "learning.reminder.due";
        }
        if (payload.containsKey("courseId") && payload.containsKey("courseTitle")) {
            return "learning.course.completed";
        }
        return null;
    }

}
