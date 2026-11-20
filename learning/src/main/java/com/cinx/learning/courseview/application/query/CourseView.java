package com.cinx.learning.courseview.application.query;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record CourseView(
        String id,
        String title,
        String description,
        CategoryView category,
        InstructorView instructor,
        List<CourseImageView> images,
        BigDecimal price,
        BigDecimal discountedPrice,
        Long discountRate,
        Double rating,
        Long enrollmentCount,
        Boolean isInSubscription,
        Long duration,
        Boolean hasCertificate,
        String certificateTitle,
        String status,
        String publishStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
