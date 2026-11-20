package com.cinx.learning.featured.api;

import com.cinx.learning.featured.api.mapper.FeaturedApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.featured.api.dto.response.FeaturedCourseResponse;
import com.cinx.learning.featured.api.dto.response.FeaturedPathDetailResponse;
import com.cinx.learning.featured.api.dto.response.FeaturedPathResponse;
import com.cinx.learning.featured.api.dto.response.FeaturedReviewResponse;
import com.cinx.learning.featured.api.dto.response.PublicStatsResponse;
import com.cinx.learning.featured.application.IFeaturedContentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public, unauthenticated reads for the guest home and catalog pages. Each path sits inside an existing
 * learning route family, so the gateway route predicates stay unchanged; both security chains permit them.
 */
@RestController
@RequiredArgsConstructor
public class FeaturedContentController {
    private final IFeaturedContentService featuredContentService;
    private final FeaturedApiMapper featuredApiMapper;

    @Operation(summary = "Featured learning paths (active, published courses only)")
    @GetMapping("/api/v1/learning/featured-paths")
    public ResponseEntity<ApiResponse<List<FeaturedPathResponse>>> getFeaturedPaths() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured paths fetched successfully",
                featuredContentService.getPublicPaths().stream().map(featuredApiMapper::toResponse).toList()));
    }

    @Operation(summary = "One featured path with its published courses in full (404 when inactive or empty)")
    @GetMapping("/api/v1/learning/featured-paths/{id}")
    public ResponseEntity<ApiResponse<FeaturedPathDetailResponse>> getFeaturedPath(@PathVariable String id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured path fetched successfully",
                featuredApiMapper.toResponse(featuredContentService.getPublicPath(id))));
    }

    @Operation(summary = "Featured courses for the home carousel")
    @GetMapping("/api/v1/courses/featured")
    public ResponseEntity<ApiResponse<List<FeaturedCourseResponse>>> getFeaturedCourses() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured courses fetched successfully",
                featuredContentService.getPublicCourses().stream().map(featuredApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Featured reviews for the home page")
    @GetMapping("/api/v1/community/reviews/featured")
    public ResponseEntity<ApiResponse<List<FeaturedReviewResponse>>> getFeaturedReviews() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured reviews fetched successfully",
                featuredContentService.getPublicReviews().stream().map(featuredApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Public platform figures (cached 10 minutes)")
    @GetMapping("/api/v1/courses/stats/public")
    public ResponseEntity<ApiResponse<PublicStatsResponse>> getPublicStats() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Public stats fetched successfully",
                featuredApiMapper.toResponse(featuredContentService.getPublicStats())));
    }
}
