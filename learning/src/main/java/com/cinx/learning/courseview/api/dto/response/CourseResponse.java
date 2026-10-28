package com.cinx.learning.courseview.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record CourseResponse(
        @Schema(example = "course_123")
        String id,
        @Schema(example = "Advanced Java")
        String title,
        @Schema(example = "Learn advanced Java topics")
        String description,
        CategoryResponse category,
        InstructorResponse instructor,
        List<CourseImageResponse> images,
        @Schema(example = "1000")
        BigDecimal price,
        @Schema(example = "800")
        BigDecimal discountedPrice,
        @Schema(example = "20")
        Long discountRate,
        @Schema(example = "4.8")
        Double rating,
        @Schema(example = "150")
        Long enrollmentCount,
        @Schema(example = "true")
        Boolean isInSubscription,
        @Schema(example = "7200")
        Long duration,
        @Schema(example = "true")
        Boolean hasCertificate,
        @Schema(example = "Java Expert Certificate")
        String certificateTitle,
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
