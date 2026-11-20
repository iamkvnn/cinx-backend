package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.CreateAssignmentLessonRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateAssignmentLessonRequest;
import com.cinx.learning.catalog.api.dto.response.AssignmentLessonResponse;
import com.cinx.learning.catalog.application.IAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons/{lessonId}/assignments")
@RequiredArgsConstructor
public class AssignmentLessonController {
    private final IAssignmentService assignmentService;
    private final CatalogApiMapper catalogApiMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<AssignmentLessonResponse>> getAssigmentByLessonId(
            @PathVariable String courseId,
            @PathVariable String lessonId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Success", catalogApiMapper.toResponse(assignmentService.getAssignmentByLessonId(currentUserId, courseId, lessonId)))
        );
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> createAssigmentLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestBody CreateAssignmentLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        assignmentService.createAssignment(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Success", null)
        );
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> updateAssigmentLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestBody UpdateAssignmentLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        assignmentService.updateAssignment(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Success", null)
        );
    }
}

