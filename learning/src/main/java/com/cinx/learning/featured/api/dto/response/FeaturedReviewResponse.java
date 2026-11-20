package com.cinx.learning.featured.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FeaturedReviewResponse(
        @Schema(example = "featured_123")
        String id,
        @Schema(example = "0")
        Integer sortOrder,
        @Schema(example = "true")
        Boolean active,
        @Schema(example = "review_123")
        String reviewId,
        @Schema(example = "5.0")
        Double rating,
        @Schema(example = "Phần xử lý lỗi giải thích rõ nhất mình từng xem.")
        String content,
        Reviewer user,
        CourseRef course
) {
    public record Reviewer(
            @Schema(example = "Hoàng Mai")
            String name,
            @Schema(example = "https://cdn.example.com/avatar.png")
            String avatarUrl
    ) {
    }

    public record CourseRef(
            @Schema(example = "course_abc123")
            String id,
            @Schema(example = "Spring Boot Microservices")
            String title
    ) {
    }
}
