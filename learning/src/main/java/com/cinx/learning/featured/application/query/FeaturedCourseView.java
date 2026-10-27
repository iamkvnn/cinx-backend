package com.cinx.learning.featured.application.query;

import com.cinx.learning.catalog.application.query.CourseView;

public record FeaturedCourseView(
        String id,
        Integer sortOrder,
        Boolean active,
        CourseView course
) {
}
