package com.cinx.learning.featured.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record FeaturedPathResponse(
        @Schema(example = "path_123")
        String id,
        @Schema(example = "Backend Java")
        String name,
        @Schema(example = "Backend Developer")
        String roleTitle,
        @Schema(example = "4")
        Integer estimatedMonths,
        @Schema(example = "5")
        Integer hoursPerWeek,
        @Schema(example = "0")
        Integer sortOrder,
        @Schema(example = "true")
        Boolean active,
        List<Course> courses,
        @Schema(example = "Đi từ nền tảng lập trình tới một hệ thống đặt hàng chạy thật.")
        String description,
        List<String> outcomes
) {
    public record Course(
            @Schema(example = "course_abc123")
            String id,
            @Schema(example = "Spring Boot Microservices")
            String title,
            @Schema(example = "https://cdn.example.com/course.png")
            String thumbnailUrl
    ) {
    }
}
