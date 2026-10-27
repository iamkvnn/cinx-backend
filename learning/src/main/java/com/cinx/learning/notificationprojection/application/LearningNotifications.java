package com.cinx.learning.notificationprojection.application;

import com.cinx.learning.notificationprojection.application.event.CertificateApprovedEvent;
import com.cinx.learning.notificationprojection.application.event.CertificateRequestedEvent;
import com.cinx.learning.notificationprojection.application.event.StudyReminderDueEvent;
import com.cinx.learning.notificationprojection.application.event.SubtitleJobFinishedEvent;
import com.cinx.learning.shared.domain.DailyGoalType;

/** Port: records the learner and instructor notification events in the caller's transaction, for the outbox relay. */
public interface LearningNotifications {
    void publishCourseCompleted(String userId, String courseId, String courseTitle);

    void publishDailyReminderDue(String userId, DailyGoalType goalType, int targetValue, int currentValue, String targetItemId);

    /** One per learner and day: the event id is derived from both, so a second one is refused by the outbox. */
    void publishStudyReminderDue(StudyReminderDueEvent event);

    void publishCertificateRequested(CertificateRequestedEvent event);

    void publishCertificateApproved(CertificateApprovedEvent event);

    /** One per job and outcome: the event id is derived from both. */
    void publishSubtitleJobFinished(SubtitleJobFinishedEvent event);
}
