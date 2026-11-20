package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.CreateLessonRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateLessonRequest;
import com.cinx.learning.catalog.api.dto.response.LessonResponse;
import com.cinx.learning.catalog.application.ILessonService;
import com.cinx.learning.catalog.application.IdempotentWrites;
import com.cinx.learning.catalog.application.query.LessonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/sections/{sectionId}/lessons")
@RequiredArgsConstructor
public class LessonController {
    private final ILessonService lessonService;
    private final CatalogApiMapper catalogApiMapper;
    private final IdempotentWrites idempotentWrites;

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @PathVariable String courseId,
            @PathVariable String sectionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        var command = catalogApiMapper.toCommand(request);
        LessonView view = idempotentWrites.once(currentUserId, idempotencyKey, "lesson.create:" + courseId + ":" + sectionId, command,
                LessonView.class, () -> lessonService.createLesson(currentUserId, courseId, sectionId, command), LessonView::id);
        return ResponseEntity.ok(ApiResponse.success("Lesson created successfully", catalogApiMapper.toResponse(view)));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{lessonId}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable String courseId,
            @PathVariable String sectionId,
            @PathVariable String lessonId,
            @RequestBody UpdateLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Lesson updated successfully",
                catalogApiMapper.toResponse(lessonService.updateLesson(currentUserId, courseId, sectionId, lessonId, catalogApiMapper.toCommand(request)))
        ));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @PathVariable String courseId,
            @PathVariable String sectionId,
            @PathVariable String lessonId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        lessonService.deleteLesson(currentUserId, courseId, sectionId, lessonId);
        return ResponseEntity.ok(ApiResponse.success("Lesson deleted successfully", null));
    }
}

