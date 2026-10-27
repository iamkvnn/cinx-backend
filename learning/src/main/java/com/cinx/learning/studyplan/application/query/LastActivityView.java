package com.cinx.learning.studyplan.application.query;

import com.cinx.learning.shared.domain.LessonType;

import java.time.LocalDateTime;

/** Where "Học tiếp" goes: the last lesson touched, or the next one in its course when that lesson is done. */
public record LastActivityView(
        String courseId,
        String courseTitle,
        String lessonId,
        String lessonTitle,
        String sectionTitle,
        LessonType lessonType,
         Integer positionSeconds,
         Integer remainingMinutes,
        LocalDateTime lastActivityAt
) {
}
