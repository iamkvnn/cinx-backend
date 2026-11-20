package com.cinx.learning.community.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CourseResponse(
        @Schema(example = "course_123")
        String id,
        @Schema(example = "Introduction to Java")
        String title,
        @Schema(example = "Learn the basics of Java programming")
        String description,
        CategoryResponse category,
        InstructorResponse instructor,
        @Schema(example = "99000")
        BigDecimal price,
        @Schema(example = "59000")
        BigDecimal discountedPrice,
        @Schema(example = "40")
        Long discountRate,
        @Schema(example = "4.8")
        Double rating,
        @Schema(example = "1500")
        Long enrollmentCount,
        @Schema(example = "false")
        Boolean isInSubscription,
        @Schema(example = "3600")
        Long duration,
        @Schema(example = "PUBLISHED")
        String status,
        @Schema(example = "PUBLISHED")
        String publishStatus,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime createdAt,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime updatedAt
) {
}
