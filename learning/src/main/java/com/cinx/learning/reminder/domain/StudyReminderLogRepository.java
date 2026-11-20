package com.cinx.learning.reminder.domain;

import java.time.LocalDate;

/** Port: the reminders already sent. */
public interface StudyReminderLogRepository {
    boolean existsByUserIdAndReminderDate(String userId, LocalDate reminderDate);

    /** Reminders a learner got between two days, both included. */
    long countByUserIdAndReminderDateBetween(String userId, LocalDate from, LocalDate to);

    /** Written at once, so a second reminder for the same learner and day fails here, inside the caller's transaction. */
    void insert(StudyReminderLog log);
}
