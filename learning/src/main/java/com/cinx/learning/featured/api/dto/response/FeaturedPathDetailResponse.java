package com.cinx.learning.featured.api.dto.response;

import com.cinx.learning.catalog.api.dto.response.CourseResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** One featured path for the public path page: the path fields plus each published course in full, in path order. */
public record FeaturedPathDetailResponse(
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
        @Schema(example = "Đi từ nền tảng lập trình tới một hệ thống đặt hàng chạy thật.")
        String description,
        List<String> outcomes,
        List<CourseResponse> courses
) {
}
