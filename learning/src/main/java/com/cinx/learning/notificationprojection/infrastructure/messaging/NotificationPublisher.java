package com.cinx.learning.notificationprojection.infrastructure.messaging;

import com.cinx.learning.notificationprojection.application.LearningNotifications;

import com.cinx.learning.shared.infrastructure.messaging.OutboxEventPublisher;

import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.notificationprojection.application.event.CertificateApprovedEvent;
import com.cinx.learning.notificationprojection.application.event.CertificateRequestedEvent;
import com.cinx.learning.notificationprojection.application.event.CourseCompletedEvent;
import com.cinx.learning.notificationprojection.application.event.DailyReminderDueEvent;
import com.cinx.learning.notificationprojection.application.event.StudyReminderDueEvent;
import com.cinx.learning.notificationprojection.application.event.SubtitleJobFinishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher implements LearningNotifications {

    private static final String EXCHANGE = "learning.events.exchange";

    private final OutboxEventPublisher outboxEventPublisher;

    public void publishCourseCompleted(String userId, String courseId, String courseTitle) {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .userId(userId)
                .courseId(courseId)
                .courseTitle(courseTitle)
                .occurredAt(Instant.now())
                .build();
        publish("learning.course.completed", event);
        log.info("CourseCompletedEvent published - userId={}, courseId={}", userId, courseId);
    }

    public void publishDailyReminderDue(
            String userId,
            DailyGoalType goalType,
            int targetValue,
            int currentValue,
            String targetItemId) {
        DailyReminderDueEvent event = DailyReminderDueEvent.builder()
                .userId(userId)
                .goalType(goalType.name())
                .targetValue(targetValue)
                .currentValue(currentValue)
                .targetItemId(targetItemId)
                .occurredAt(Instant.now())
                .build();
        publish("learning.reminder.due", event);
        log.info("DailyReminderDueEvent published - userId={}, goalType={}", userId, goalType);
    }

    @Override
    public void publishStudyReminderDue(StudyReminderDueEvent event) {
        String eventId = "study-reminder:" + event.getUserId() + ":" + event.getReminderDate();
        outboxEventPublisher.enqueue(eventId, "StudyReminder", event.getUserId(), "learning.study-reminder.due",
                EXCHANGE, "learning.study-reminder.due", event);
        log.info("StudyReminderDueEvent published - userId={}, reason={}, writtenBy={}",
                event.getUserId(), event.getReason(), event.getWrittenBy());
    }

    @Override
    public void publishSubtitleJobFinished(SubtitleJobFinishedEvent event) {
        String eventId = "subtitle-job:" + event.getJobId() + ":" + event.getOutcome();
        outboxEventPublisher.enqueue(eventId, "SubtitleJob", event.getJobId(), "learning.subtitle.job.finished",
                EXCHANGE, "learning.subtitle.job.finished", event);
        log.info("SubtitleJobFinishedEvent published - jobId={}, outcome={}, warnings={}",
                event.getJobId(), event.getOutcome(), event.getWarningCount());
    }

    public void publishCertificateRequested(CertificateRequestedEvent event) {
        publish("learning.certificate.requested", event);
        log.info("CertificateRequestedEvent published - requestId={}, instructorId={}",
                event.getRequestId(), event.getInstructorId());
    }

    public void publishCertificateApproved(CertificateApprovedEvent event) {
        publish("learning.certificate.approved", event);
        log.info("CertificateApprovedEvent published - requestId={}, userId={}",
                event.getRequestId(), event.getUserId());
    }

    private void publish(String routingKey, Object event) {
        outboxEventPublisher.enqueue(
                UUID.randomUUID().toString(),
                "LearningNotification",
                routingKey,
                routingKey,
                EXCHANGE,
                routingKey,
                event
        );
    }
}
