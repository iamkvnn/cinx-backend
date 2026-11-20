package com.cinx.learning.featured.application.query;

/**
 * Platform figures for the public home page, computed inside learning only:
 * learners = distinct users with a course enrollment, instructors = distinct owners of published courses.
 */
public record PublicStatsView(
        long learnerCount,
        long instructorCount,
        long courseCount
) {
}
