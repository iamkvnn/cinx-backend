package com.cinx.learning.catalog.api;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import java.math.BigDecimal;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.api.dto.request.RejectCourseRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateCourseAccessPolicyRequest;
import com.cinx.learning.catalog.api.dto.response.CourseAccessPolicyResponse;
import com.cinx.learning.catalog.api.dto.response.CourseCurriculumResponse;
import com.cinx.learning.catalog.api.dto.response.CourseReadiness;
import com.cinx.learning.catalog.api.dto.response.CourseResponse;
import com.cinx.learning.catalog.api.dto.response.DraftChanges;
import com.cinx.learning.catalog.application.CourseAccessPolicyService;
import com.cinx.learning.catalog.application.ICourseChangeService;
import com.cinx.learning.catalog.application.ICourseService;
import com.cinx.learning.catalog.application.ICurriculumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {
    private final ICourseService courseService;
    private final ICurriculumService curriculumService;
    private final ICourseChangeService courseChangeService;
    private final CourseAccessPolicyService accessPolicyService;
    private final CatalogApiMapper catalogApiMapper;

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_learning.course.review.any')")
    public ResponseEntity<PaginatedApiResponse<CourseResponse>> getAllCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) BigDecimal priceFrom,
            @RequestParam(required = false) BigDecimal priceTo,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) CoursePublishStatus publishStatus,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String instructorId
    ) {
        PageResult<CourseResponse> courses = courseService.getAllCourses(query, categoryId, instructorId, rating, priceFrom, priceTo, status, publishStatus, page, size, sort).map(catalogApiMapper::toResponse);
        return ResponseEntity.ok(PaginationWrapper.wrap(courses));
    }

    @Operation(summary = "The submitted draft, for review", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}/draft")
    @PreAuthorize("hasAuthority('PERM_learning.course.review.any')")
    public ResponseEntity<ApiResponse<CourseResponse>> getDraftForReview(@PathVariable("id") String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course draft fetched successfully",
                catalogApiMapper.toResponse(courseService.getDraftForReview(courseId))));
    }

    @Operation(summary = "The submitted draft's curriculum, for review", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}/draft/curriculum")
    @PreAuthorize("hasAuthority('PERM_learning.course.review.any')")
    public ResponseEntity<ApiResponse<CourseCurriculumResponse>> getDraftCurriculumForReview(@PathVariable("id") String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course curriculum fetched successfully",
                catalogApiMapper.toResponse(curriculumService.getDraftCurriculumForReview(courseId))));
    }

    @Operation(summary = "What the submitted draft changes against the live course", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}/draft/changes")
    @PreAuthorize("hasAuthority('PERM_learning.course.review.any')")
    public ResponseEntity<ApiResponse<DraftChanges>> getDraftChangesForReview(@PathVariable("id") String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Draft changes fetched successfully",
                catalogApiMapper.toResponse(courseChangeService.getDraftChangesForReview(courseId))));
    }

    @Operation(summary = "What is still missing in the submitted draft", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}/readiness")
    @PreAuthorize("hasAuthority('PERM_learning.course.review.any')")
    public ResponseEntity<ApiResponse<CourseReadiness>> getReadinessForReview(@PathVariable("id") String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course readiness fetched successfully",
                catalogApiMapper.toResponse(courseService.getReadinessForReview(courseId))));
    }

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_learning.course.review.any') and hasAuthority('PERM_learning.course.publish.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<CourseResponse>> approveCourse(@PathVariable("id") String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course approved successfully", catalogApiMapper.toResponse(courseService.approveCourse(courseId))));
    }

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PERM_learning.course.review.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<CourseResponse>> rejectCourse(
            @PathVariable("id") String courseId,
            @jakarta.validation.Valid @RequestBody RejectCourseRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course rejected successfully", catalogApiMapper.toResponse(courseService.rejectCourse(courseId, catalogApiMapper.toCommand(request)))));
    }

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/recommendation/replay")
    @PreAuthorize("hasAuthority('PERM_learning.course.review.any')")
    public ResponseEntity<ApiResponse<Void>> replayRecommendationEvents() {
        courseService.replayRecommendationEvents();
        return ResponseEntity.ok(new ApiResponse<>(true, "Course recommendation replay enqueued successfully", null));
    }

    @Operation(summary = "Update course access policy", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{id}/access-policy")
    @PreAuthorize("hasAuthority('PERM_learning.course.subscription-policy.manage.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<CourseAccessPolicyResponse>> updateAccessPolicy(
            @PathVariable("id") String courseId,
            @Valid @RequestBody UpdateCourseAccessPolicyRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course access policy updated successfully",
                catalogApiMapper.toResponse(accessPolicyService.update(courseId, catalogApiMapper.toCommand(request)))));
    }
}

