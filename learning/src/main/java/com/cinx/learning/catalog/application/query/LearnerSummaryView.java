package com.cinx.learning.catalog.application.query;

/** What a course's instructor may see about someone taking part in that course: no email, no contact data. */
public record LearnerSummaryView(
        String userId,
        String name,
        String avatarUrl
) {
}
