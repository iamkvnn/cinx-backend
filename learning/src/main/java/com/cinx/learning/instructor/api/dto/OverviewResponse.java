package com.cinx.learning.instructor.api.dto;

import java.time.LocalDate;
import java.util.List;

/** The instructor overview for a period, each number with the same-length period before it. */
public record OverviewResponse(
        long newLearners,
        long newLearnersPrevious,
        long totalLearners,
        Double rating,
        Double ratingPrevious,
        long reviews,
        long unrepliedReviews,
        long waiting,
        long overdue,
        List<DayCount> newLearnersByDay,
        List<CourseRow> courses
) {
    public record DayCount(LocalDate date, long count) {
    }

    public record CourseRow(
            String courseId,
            String title,
            String thumbnailUrl,
            String status,
            String publishStatus,
            long learners,
            long newLearners,
            Integer completionPercent,
            Double rating,
            long waiting
    ) {
    }
}
