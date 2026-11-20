package com.cinx.learning.featured.application.query;

import com.cinx.learning.catalog.application.query.CourseView;

import java.util.List;

/** One featured path for the public path page: the path fields plus each published course in full, in path order. */
public record FeaturedPathDetailView(
        String id,
        String name,
        String roleTitle,
        Integer estimatedMonths,
        Integer hoursPerWeek,
        String description,
        List<String> outcomes,
        List<CourseView> courses
) {
}
