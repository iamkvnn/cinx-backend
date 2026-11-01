package com.cinx.learning.reminder.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** On the hour from 08:00 to 20:00, Vietnam time: the reminders of the learners who chose that hour. */
@Component
@RequiredArgsConstructor
public class StudyReminderJob {
    private final StudyReminderService reminders;

    @Scheduled(cron = "0 0 8-20 * * *", zone = "Asia/Ho_Chi_Minh")
    public void run() {
        reminders.runHour();
    }
}
