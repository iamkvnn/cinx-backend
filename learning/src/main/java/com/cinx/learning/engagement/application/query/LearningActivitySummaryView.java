package com.cinx.learning.engagement.application.query;

import java.time.LocalDate;
import java.util.List;

/** The caller's own study time: everything so far, and each day of the current week (Monday first). */
public record LearningActivitySummaryView(
        long totalSeconds,
        LocalDate weekStart,
        List<Day> days
) {
    public record Day(
            LocalDate date,
            long seconds
    ) {
    }
}
