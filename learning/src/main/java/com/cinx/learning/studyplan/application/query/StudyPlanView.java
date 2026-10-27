package com.cinx.learning.studyplan.application.query;

import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.studyplan.domain.StudyPlanSource;

import java.time.LocalDate;
import java.util.List;

/** The plan and one week of it, as "Tuần của tôi" renders it. */
public record StudyPlanView(
        StudyPlanSource source,
         Integer minutesPerDay,
         List<Integer> freeDays,
        LocalDate today,
        LocalDate startDate,
        LocalDate finishDate,
        LocalDate baselineFinishDate,
         long lateDays,
         int missedDaysLastWeek,
        int remainingLessons,
        int remainingMinutes,
         String pathTitle,
        List<Course> courses,
        Week week
) {
    public record Course(String courseId, String title, String thumbnailUrl, int completedItems, int totalItems) {
    }

    public record Week(LocalDate from, LocalDate to, List<Day> days) {
    }

    public record Day(LocalDate date, boolean free, boolean dayOff, int capacityMinutes, List<Session> sessions) {
    }

    public record Session(
            String courseId,
            String courseTitle,
            String lessonId,
            String lessonTitle,
            String sectionTitle,
            LessonType lessonType,
            int minutes,
             String status) {
    }
}
