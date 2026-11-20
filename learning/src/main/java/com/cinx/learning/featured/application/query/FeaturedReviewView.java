package com.cinx.learning.featured.application.query;

public record FeaturedReviewView(
        String id,
        Integer sortOrder,
        Boolean active,
        String reviewId,
        Double rating,
        String content,
        Reviewer user,
        CourseRef course
) {
    public record Reviewer(
            String name,
            String avatarUrl
    ) {
    }

    public record CourseRef(
            String id,
            String title
    ) {
    }
}
