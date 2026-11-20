package com.cinx.learning.reminder.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/** The reminder a learner got on a day; there is at most one per learner and day. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyReminderLog {
    private String id;
    private String userId;
    private LocalDate reminderDate;
    private ReminderReason reason;
    private ReminderAuthor writtenBy;
    private Instant createdAt;
}
