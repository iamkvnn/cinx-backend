package com.cinx.learning.featured.application.query;

import java.util.List;

public record FeaturedPathView(
        String id,
        String name,
        String roleTitle,
        Integer estimatedMonths,
        Integer hoursPerWeek,
        Integer sortOrder,
        Boolean active,
        List<Course> courses,
        String description,
        List<String> outcomes
) {
    public record Course(
            String id,
            String title,
            String thumbnailUrl
    ) {
    }
}
