package com.cinx.learning.catalog.api;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.learning.catalog.api.dto.response.CourseCurriculumResponse;
import com.cinx.learning.catalog.api.dto.response.CourseReadiness;
import com.cinx.learning.catalog.api.dto.response.CourseResponse;
import com.cinx.learning.catalog.api.dto.response.DraftChanges;
import com.cinx.learning.catalog.api.dto.response.LessonPositionResponse;
import com.cinx.learning.catalog.api.dto.response.RejectCourseResponse;
import com.cinx.learning.catalog.api.dto.response.ReviewNoteResponse;
import com.cinx.learning.catalog.api.dto.response.SectionPositionResponse;

import java.math.BigDecimal;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.api.dto.request.CreateCourseRequest;
import com.cinx.learning.catalog.api.dto.request.MoveLessonRequest;
import com.cinx.learning.catalog.api.dto.request.MoveSectionRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateCourseRequest;
import com.cinx.learning.catalog.application.ICourseChangeService;
import com.cinx.learning.catalog.application.ICourseDeletionService;
import com.cinx.learning.catalog.application.ICourseService;
import com.cinx.learning.catalog.application.ICurriculumService;
import com.cinx.learning.catalog.application.ILessonService;
import com.cinx.learning.catalog.application.ISectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
    private final ICourseService courseService;
    private final ICourseDeletionService courseDeletionService;
    private final ICourseChangeService courseChangeService;
    private final ICurriculumService curriculumService;
    private final ISectionService sectionService;
    private final ILessonService lessonService;
    private final CatalogApiMapper catalogApiMapper;

    @GetMapping
    public ResponseEntity<PaginatedApiResponse<CourseResponse>> getAllCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) BigDecimal priceFrom,
            @RequestParam(required = false) BigDecimal priceTo,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) Boolean hasCertificate,
            @RequestParam(required = false) Boolean onSale,
            // course length in minutes: from inclusive, to exclusive, so adjacent catalog buckets never overlap
            @RequestParam(required = false) Long durationFrom,
            @RequestParam(required = false) Long durationTo
    ) {
        PageResult<CourseResponse> courses = courseService.getAllPublishedCourses(query, categoryId, instructorId, rating, priceFrom, priceTo, hasCertificate, onSale, durationFrom, durationTo, page, size, sort).map(catalogApiMapper::toResponse);
        return ResponseEntity.ok(PaginationWrapper.wrap(courses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getReadableCourseById(@PathVariable("id") String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok().body(
                new ApiResponse<>(true, "Course fetched successfully", catalogApiMapper.toResponse(courseService.getPublicCourseById(currentUserId, courseId)))
        );
    }

    @Operation(summary = "Get my courses", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/mine")
    public ResponseEntity<PaginatedApiResponse<CourseResponse>> getMyCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) BigDecimal priceFrom,
            @RequestParam(required = false) BigDecimal priceTo,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) CoursePublishStatus publishStatus,
            @RequestParam(required = false) String categoryId
    ) {
        PageResult<CourseResponse> courses = courseService.getAllCourses(query, categoryId, AuthenticationUtil.extractUserId(), rating, priceFrom, priceTo, status, publishStatus, page, size, sort).map(catalogApiMapper::toResponse);
        return ResponseEntity.ok(PaginationWrapper.wrap(courses));
    }

    @Operation(summary = "Get editable course draft", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}/draft")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseResponse>> getEditableCourseDraft(@PathVariable("id") String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Course draft fetched successfully",
                catalogApiMapper.toResponse(courseService.getEditableDraftCourseById(currentUserId, courseId))
        ));
    }

    @Operation(summary = "What the draft changes against the live course", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}/draft/changes")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<DraftChanges>> getDraftChanges(@PathVariable("id") String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Draft changes fetched successfully",
                catalogApiMapper.toResponse(courseChangeService.getDraftChanges(AuthenticationUtil.extractUserId(), courseId))
        ));
    }

    @Operation(summary = "What must be fixed before the course can be sent for review", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}/readiness")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseReadiness>> getReadiness(@PathVariable("id") String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Course readiness fetched successfully",
                catalogApiMapper.toResponse(courseService.getReadiness(AuthenticationUtil.extractUserId(), courseId))
        ));
    }

    @GetMapping("/{id}/curriculum")
    public ResponseEntity<ApiResponse<CourseCurriculumResponse>> getReadableCurriculum(@PathVariable("id") String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Course curriculum fetched successfully",
                catalogApiMapper.toResponse(curriculumService.getPublicCurriculum(currentUserId, courseId))
        ));
    }

    @Operation(summary = "Get editable course curriculum", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}/draft/curriculum")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseCurriculumResponse>> getEditableDraftCurriculum(@PathVariable("id") String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Course draft curriculum fetched successfully",
                catalogApiMapper.toResponse(curriculumService.getEditableDraftCurriculum(currentUserId, courseId))
        ));
    }

    @Operation(summary = "Move editable course section", security = @SecurityRequirement(name = "bearer-jwt"))
    @PatchMapping("/{id}/curriculum/sections/{sectionId}/position")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<SectionPositionResponse>> moveSection(
            @PathVariable String id,
            @PathVariable String sectionId,
            @Valid @RequestBody MoveSectionRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Section position updated successfully",
                catalogApiMapper.toResponse(sectionService.moveSection(currentUserId, id, sectionId, catalogApiMapper.toCommand(request)))
        ));
    }

    @Operation(summary = "Move editable course lesson", security = @SecurityRequirement(name = "bearer-jwt"))
    @PatchMapping("/{id}/curriculum/lessons/{lessonId}/position")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<LessonPositionResponse>> moveLesson(
            @PathVariable String id,
            @PathVariable String lessonId,
            @Valid @RequestBody MoveLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lesson position updated successfully",
                catalogApiMapper.toResponse(lessonService.moveLesson(currentUserId, id, lessonId, catalogApiMapper.toCommand(request)))
        ));
    }

    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCoursesByIds(@RequestParam("ids") List<String> courseIds) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok().body(
                new ApiResponse<>(true, "Course fetched successfully", courseService.getReadableCourseByIds(currentUserId, courseIds).stream().map(catalogApiMapper::toResponse).toList())
        );
    }

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.course.create')")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok().body(
                new ApiResponse<>(true, "Course created successfully", catalogApiMapper.toResponse(courseService.createCourse(currentUserId, catalogApiMapper.toCommand(request))))
        );
    }

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(@PathVariable("id") String courseId, @Valid @RequestBody UpdateCourseRequest request) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok().body(
                new ApiResponse<>(true, "Course updated successfully", catalogApiMapper.toResponse(courseService.updateCourse(currentUserId, courseId, catalogApiMapper.toCommand(request))))
        );
    }

    @Operation(summary = "Submit course for approval", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseResponse>> submitCourse(@PathVariable("id") String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok().body(
                new ApiResponse<>(true, "Course submitted successfully", catalogApiMapper.toResponse(courseService.submitCourse(currentUserId, courseId)))
        );
    }

    @Operation(summary = "Withdraw a course from review", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseResponse>> withdrawCourse(@PathVariable("id") String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Course withdrawn from review",
                catalogApiMapper.toResponse(courseService.withdrawCourse(currentUserId, courseId))
        ));
    }

    @Operation(summary = "Delete a draft course forever (owner only, never published, no learners)", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<Void> deleteDraftCourse(@PathVariable("id") String courseId) {
        courseDeletionService.deleteDraftCourse(AuthenticationUtil.extractUserId(), courseId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Archive course", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseResponse>> archiveCourse(@PathVariable("id") String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Course archived successfully",
                catalogApiMapper.toResponse(courseService.archiveCourse(currentUserId, courseId))
        ));
    }

    @Operation(summary = "Unarchive course", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseResponse>> unarchiveCourse(@PathVariable("id") String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Course unarchived successfully",
                catalogApiMapper.toResponse(courseService.unarchiveCourse(currentUserId, courseId))
        ));
    }

    @GetMapping("/{id}/reject-reason")
    public ResponseEntity<ApiResponse<RejectCourseResponse>> getRejectReason(@PathVariable("id") String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok().body(
                new ApiResponse<>(true, "Reject reason fetched successfully", catalogApiMapper.toResponse(courseService.getRejectReason(currentUserId, courseId)))
        );
    }

    @Operation(summary = "The last rejection's reason and located review notes (course team only)", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}/review-notes")
    public ResponseEntity<ApiResponse<ReviewNoteResponse.Rejection>> getReviewNotes(@PathVariable("id") String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("", catalogApiMapper.toResponse(courseService.getReviewNotes(currentUserId, courseId))));
    }
}

