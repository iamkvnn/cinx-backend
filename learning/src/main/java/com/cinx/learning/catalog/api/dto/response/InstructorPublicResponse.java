package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/** The public page of one verified instructor. It never carries the email, phone, CV or application fields. */
public record InstructorPublicResponse(
        @Schema(description = "Same id as CourseResponse.instructor.id", example = "usr_123")
        String id,
        @Schema(example = "Nguyen Van A")
        String name,
        @Schema(example = "https://example.com/profiles/123.jpg")
        String avatarUrl,
        @Schema(description = "Plain text, up to 2000 characters, line breaks separate paragraphs", example = "Tech Lead backend, 11 years.")
        String bio,
        @Schema(description = "Date the profile was created", example = "2025-03-01")
        LocalDate joinedAt,
        @Schema(description = "Published courses the instructor owns or is an active member of", example = "6")
        long publishedCourseCount,
        @Schema(description = "Sum of enrollments over those courses; a learner in two courses counts twice", example = "3504")
        long enrollmentCount,
        @Schema(description = "Average rating weighted by each course's rating count; null when nothing is rated", example = "4.84")
        Double ratingAverage,
        @Schema(description = "Sum of rating counts over those courses", example = "909")
        long ratingCount,
        @Schema(description = "Categories of those courses, most courses first")
        List<Category> categories
) {
    public record Category(
            @Schema(example = "cat_123") String id,
            @Schema(example = "Backend") String name,
            @Schema(example = "3") long courseCount
    ) {
    }
}
