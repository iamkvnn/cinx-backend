package com.cinx.learning.progress.application.query;

import java.util.List;

public record CoursesProgressSummaryView(
        Long totalStudentProgressCount,
        Long completedStudentProgressCount,
        Double completionRate,
        List<CourseProgressSummaryView> courses
) {
}
