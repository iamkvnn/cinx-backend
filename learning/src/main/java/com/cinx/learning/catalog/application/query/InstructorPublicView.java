package com.cinx.learning.catalog.application.query;

import java.time.LocalDate;
import java.util.List;

/** The public page of one verified instructor. It never carries the email, phone, CV or application fields. */
public record InstructorPublicView(
        String id,
        String name,
        String avatarUrl,
        String bio,
        LocalDate joinedAt,
        long publishedCourseCount,
        long enrollmentCount,
        Double ratingAverage,
        long ratingCount,
        List<Category> categories
) {
    public record Category(
             String id,
             String name,
             long courseCount
    ) {
    }
}
