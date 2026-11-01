package com.cinx.learning.reminder.application;

import com.cinx.learning.notificationprojection.application.LearningNotifications;
import com.cinx.learning.notificationprojection.application.event.StudyReminderDueEvent;
import com.cinx.learning.reminder.domain.ReminderAuthor;
import com.cinx.learning.reminder.domain.StudyReminderLog;
import com.cinx.learning.reminder.domain.StudyReminderLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** The log row and the outbox event of one reminder, in one transaction: a duplicate day rolls both back. */
@Service
@RequiredArgsConstructor
public class StudyReminderSender {
    private final Clock clock;
    private final StudyReminderLogRepository logs;
    private final LearningNotifications notifications;

    @Transactional
    public void send(String userId, LocalDate day, ReminderMessage message, ReminderAuthor author) {
        logs.insert(StudyReminderLog.builder()
                .id(UUID.randomUUID().toString()).userId(userId).reminderDate(day).reason(message.reason())
                .writtenBy(author).createdAt(clock.instant()).build());
        notifications.publishStudyReminderDue(StudyReminderDueEvent.builder()
                .userId(userId).reason(message.reason().name()).title(message.title()).body(message.body())
                .actionUrl(message.actionUrl()).writtenBy(author.name()).reminderDate(day).occurredAt(clock.instant())
                .build());
    }
}
