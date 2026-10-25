package com.cinx.learning.catalog.application.query;

public record InstructorCourseSummaryView(
        Long courseCount,
        Long publishedCourseCount,
        Double averageRating
) {
}

