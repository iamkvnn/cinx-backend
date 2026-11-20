package com.cinx.learning.studyplan.api.dto.response;

import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.studyplan.domain.StudyPlanSource;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/** The plan and one week of it, as "Tuần của tôi" renders it. */
public record StudyPlanResponse(
        StudyPlanSource source,
        @Schema(example = "30") Integer minutesPerDay,
        @Schema(example = "[1,2,4,5,6]") List<Integer> freeDays,
        LocalDate today,
        LocalDate startDate,
        LocalDate finishDate,
        LocalDate baselineFinishDate,
        @Schema(description = "finishDate − baselineFinishDate in days, never negative") long lateDays,
        @Schema(description = "Free, not-busy days of the last 7 with nothing completed") int missedDaysLastWeek,
        int remainingLessons,
        int remainingMinutes,
        @Schema(description = "Path title when source = PATH") String pathTitle,
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
            @Schema(example = "PLANNED", allowableValues = {"DONE", "PLANNED"}) String status) {
    }
}
