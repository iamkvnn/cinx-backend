package com.cinx.learning.community.application.query;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CourseView(
        String id,
        String title,
        String description,
        CategoryView category,
        InstructorView instructor,
        BigDecimal price,
        BigDecimal discountedPrice,
        Long discountRate,
        Double rating,
        Long enrollmentCount,
        Boolean isInSubscription,
        Long duration,
        String status,
        String publishStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
