package com.cinx.learning.progress.api;

import com.cinx.learning.progress.api.mapper.ProgressApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.progress.api.dto.response.CourseLocksResponse;
import com.cinx.learning.progress.api.dto.response.CourseProgressResponse;
import com.cinx.learning.progress.application.IPrerequisiteService;
import com.cinx.learning.progress.api.dto.response.LearningItemProgressResponse;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import com.cinx.learning.progress.application.ILearningProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/learning/course-progress")
public class LearningProgressController {
    private final ILearningProgressService learningProgressService;
    private final LearningAuthorizationService authorizationService;
    private final IPrerequisiteService prerequisiteService;
    private final ProgressApiMapper progressApiMapper;

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.self')")
    public ResponseEntity<ApiResponse<List<CourseProgressResponse>>> getCourseProgressByCourseIds(@RequestParam List<String> courseIds) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "", learningProgressService.getCourseProgressByCourseIds(userId, courseIds).stream().map(progressApiMapper::toResponse).toList()));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{courseId}")
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.self')")
    public ResponseEntity<ApiResponse<CourseProgressResponse>> getMyCourseProgress(@PathVariable String courseId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "", progressApiMapper.toResponse(learningProgressService.getCourseProgress(userId, courseId))));
    }

    @Operation(summary = "Lessons of the course locked for me by prerequisites, with what is missing", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{courseId}/locks")
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.self')")
    public ResponseEntity<ApiResponse<CourseLocksResponse>> getLocks(@PathVariable String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Locks fetched successfully",
                new CourseLocksResponse(prerequisiteService.locks(AuthenticationUtil.extractUserId(), courseId))));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{courseId}/items")
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.self')")
    public ResponseEntity<ApiResponse<List<LearningItemProgressResponse>>> getLearningItemProgressByCourseId(@PathVariable String courseId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "", learningProgressService.getLearningItemProgressByCourseId(userId, courseId).stream().map(progressApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Mark an item as complete (e.g. Article)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/items/{itemId}/complete")
    @PreAuthorize("hasAuthority('PERM_learning.progress.write.self')")
    public ResponseEntity<ApiResponse<?>> markItemAsComplete(@PathVariable String itemId) {
        String userId = AuthenticationUtil.extractUserId();
        learningProgressService.completeArticleItem(userId, itemId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Marked complete", null));
    }

    @Operation(summary = "Get overview progress of students in a course", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/courses/{courseId}/progress")
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.own-course') or hasAuthority('PERM_learning.analytics.read.any')")
    public ResponseEntity<ApiResponse<List<CourseProgressResponse>>> getCourseProgress(@PathVariable String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        authorizationService.requireCourseInstructor(currentUserId, courseId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", learningProgressService.getCourseProgressByCourseId(courseId).stream().map(progressApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Get detailed progress of a student in a course", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/courses/{courseId}/students/{studentId}/progress")
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.own-course') or hasAuthority('PERM_learning.analytics.read.any')")
    public ResponseEntity<ApiResponse<List<LearningItemProgressResponse>>> getStudentProgress(
            @PathVariable String courseId,
            @PathVariable String studentId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        authorizationService.requireCourseInstructor(currentUserId, courseId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", learningProgressService.getLearningItemProgressByCourseId(studentId, courseId).stream().map(progressApiMapper::toResponse).toList()));
    }
}
