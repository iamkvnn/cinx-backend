package com.cinx.learning.featured.api;

import com.cinx.learning.featured.api.mapper.FeaturedApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.featured.api.dto.request.AddFeaturedItemRequest;
import com.cinx.learning.featured.api.dto.request.ReorderRequest;
import com.cinx.learning.featured.api.dto.request.UpdateFeaturedItemRequest;
import com.cinx.learning.featured.api.dto.request.UpsertFeaturedPathRequest;
import com.cinx.learning.featured.api.dto.response.FeaturedCourseResponse;
import com.cinx.learning.featured.api.dto.response.FeaturedPathResponse;
import com.cinx.learning.featured.api.dto.response.FeaturedReviewResponse;
import com.cinx.learning.featured.application.IFeaturedContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin management of featured paths, courses and reviews. Reads need the permission; writes also need MFA. */
@RestController
@RequestMapping("/api/v1/learning/admin/featured")
@RequiredArgsConstructor
public class AdminFeaturedContentController {
    private static final String READ = "hasAuthority('PERM_learning.featured.manage.any')";
    private static final String WRITE = "hasAuthority('PERM_learning.featured.manage.any') and @cinxMfaVerifier.requireSatisfied(authentication)";

    private final IFeaturedContentService service;
    private final FeaturedApiMapper featuredApiMapper;

    // ---- paths

    @Operation(summary = "List featured paths", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/paths")
    @PreAuthorize(READ)
    public ResponseEntity<ApiResponse<List<FeaturedPathResponse>>> getPaths() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured paths fetched successfully", service.getAllPaths().stream().map(featuredApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Create a featured path", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/paths")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<FeaturedPathResponse>> createPath(@Valid @RequestBody UpsertFeaturedPathRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured path created successfully", featuredApiMapper.toResponse(service.createPath(featuredApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Update a featured path", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/paths/{id}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<FeaturedPathResponse>> updatePath(@PathVariable String id, @Valid @RequestBody UpsertFeaturedPathRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured path updated successfully", featuredApiMapper.toResponse(service.updatePath(id, featuredApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Delete a featured path", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/paths/{id}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<Void>> deletePath(@PathVariable String id) {
        service.deletePath(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured path deleted successfully", null));
    }

    @Operation(summary = "Reorder featured paths", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/paths/order")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<List<FeaturedPathResponse>>> reorderPaths(@Valid @RequestBody ReorderRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured paths reordered successfully", service.reorderPaths(featuredApiMapper.toCommand(request)).stream().map(featuredApiMapper::toResponse).toList()));
    }

    // ---- courses

    @Operation(summary = "List featured courses", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/courses")
    @PreAuthorize(READ)
    public ResponseEntity<ApiResponse<List<FeaturedCourseResponse>>> getCourses() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured courses fetched successfully", service.getAllCourses().stream().map(featuredApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Feature a course", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/courses")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<FeaturedCourseResponse>> addCourse(@Valid @RequestBody AddFeaturedItemRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course featured successfully", featuredApiMapper.toResponse(service.addCourse(featuredApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Show or hide a featured course", security = @SecurityRequirement(name = "bearer-jwt"))
    @PatchMapping("/courses/{id}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<FeaturedCourseResponse>> updateCourse(@PathVariable String id, @Valid @RequestBody UpdateFeaturedItemRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured course updated successfully", featuredApiMapper.toResponse(service.updateCourse(id, featuredApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Remove a featured course", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/courses/{id}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<Void>> removeCourse(@PathVariable String id) {
        service.removeCourse(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured course removed successfully", null));
    }

    @Operation(summary = "Reorder featured courses", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/courses/order")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<List<FeaturedCourseResponse>>> reorderCourses(@Valid @RequestBody ReorderRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured courses reordered successfully", service.reorderCourses(featuredApiMapper.toCommand(request)).stream().map(featuredApiMapper::toResponse).toList()));
    }

    // ---- reviews

    @Operation(summary = "List featured reviews", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/reviews")
    @PreAuthorize(READ)
    public ResponseEntity<ApiResponse<List<FeaturedReviewResponse>>> getReviews() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured reviews fetched successfully", service.getAllReviews().stream().map(featuredApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Feature a review", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/reviews")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<FeaturedReviewResponse>> addReview(@Valid @RequestBody AddFeaturedItemRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Review featured successfully", featuredApiMapper.toResponse(service.addReview(featuredApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Show or hide a featured review", security = @SecurityRequirement(name = "bearer-jwt"))
    @PatchMapping("/reviews/{id}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<FeaturedReviewResponse>> updateReview(@PathVariable String id, @Valid @RequestBody UpdateFeaturedItemRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured review updated successfully", featuredApiMapper.toResponse(service.updateReview(id, featuredApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Remove a featured review", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/reviews/{id}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<Void>> removeReview(@PathVariable String id) {
        service.removeReview(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured review removed successfully", null));
    }

    @Operation(summary = "Reorder featured reviews", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/reviews/order")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<List<FeaturedReviewResponse>>> reorderReviews(@Valid @RequestBody ReorderRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Featured reviews reordered successfully", service.reorderReviews(featuredApiMapper.toCommand(request)).stream().map(featuredApiMapper::toResponse).toList()));
    }
}
