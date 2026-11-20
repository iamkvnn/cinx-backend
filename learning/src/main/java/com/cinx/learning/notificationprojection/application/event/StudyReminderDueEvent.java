package com.cinx.learning.notificationprojection.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/** learning.study-reminder.due v1: the reminder is chosen and written here; notification only delivers it. */
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
    /** TEMPLATE or AI */
    private String writtenBy;
    private LocalDate reminderDate;
    private Instant occurredAt;
}
