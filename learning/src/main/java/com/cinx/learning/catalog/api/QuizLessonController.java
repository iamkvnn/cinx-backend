package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.CreateQuizLessonRequest;
import com.cinx.learning.catalog.api.dto.request.SyncQuizRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateQuizLessonRequest;
import com.cinx.learning.catalog.api.dto.response.QuizLessonResponse;
import com.cinx.learning.catalog.application.IQuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons/{lessonId}/quizzes")
@RequiredArgsConstructor
public class QuizLessonController {
    private final IQuizService quizService;
    private final CatalogApiMapper catalogApiMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<QuizLessonResponse>> getQuizByLessonId(
            @PathVariable String courseId,
            @PathVariable String lessonId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", catalogApiMapper.toResponse(quizService.getReadableQuizByLessonId(currentUserId, courseId, lessonId))));
    }

    @Operation(summary = "Create quiz with initial questions", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> createQuizLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @Valid @RequestBody CreateQuizLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        quizService.createQuiz(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Quiz created successfully", null));
    }

    @Operation(summary = "Update quiz (no questions)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> updateQuizSettings(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @Valid @RequestBody UpdateQuizLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        quizService.updateQuiz(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Quiz updated successfully", null));
    }

    @Operation(summary = "Sync quiz changes to learning service (with optional regrade trigger)",
               security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> syncQuiz(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @Valid @RequestBody SyncQuizRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        quizService.syncQuiz(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true,
                Boolean.TRUE.equals(request.triggerRegrade())
                        ? "Quiz synced and regrade triggered"
                        : "Quiz sync acknowledged (no regrade)",
                null));
    }
}

