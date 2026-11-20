package com.cinx.notification.notification.infrastructure.messaging.event.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** learning.study-reminder.due v1: learning chose and wrote the reminder; notification only delivers it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyReminderDueEvent {
    private String userId;
    private String reason;
    private String title;
    private String body;
    private String actionUrl;
    private String writtenBy;
    /** ISO date in Asia/Ho_Chi_Minh; only carried through, so it stays text. */
    private String reminderDate;
    private Instant occurredAt;
}
