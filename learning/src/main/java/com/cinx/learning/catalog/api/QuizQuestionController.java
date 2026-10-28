package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.CreateQuizQuestionRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateQuizQuestionRequest;
import com.cinx.learning.catalog.api.dto.response.QuizQuestionResponse;
import com.cinx.learning.catalog.application.IQuizQuestionService;
import com.cinx.learning.catalog.application.IdempotentWrites;
import com.cinx.learning.catalog.application.query.QuizQuestionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons/{lessonId}/quizzes/questions")
@RequiredArgsConstructor
public class QuizQuestionController {

    private final IQuizQuestionService quizQuestionService;
    private final CatalogApiMapper catalogApiMapper;
    private final IdempotentWrites idempotentWrites;

    @Operation(summary = "List all questions for a quiz", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public ResponseEntity<ApiResponse<List<QuizQuestionResponse>>> getQuestions(
            @PathVariable String lessonId,
            @PathVariable String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", quizQuestionService.getQuestions(currentUserId, courseId, lessonId).stream().map(catalogApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Add a question to a quiz", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<QuizQuestionResponse>> addQuestion(
            @PathVariable String lessonId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateQuizQuestionRequest request,
            @PathVariable String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        var command = catalogApiMapper.toCommand(request);
        QuizQuestionView view = idempotentWrites.once(currentUserId, idempotencyKey, "quiz-question.create:" + courseId + ":" + lessonId,
                command, QuizQuestionView.class, () -> quizQuestionService.addQuestion(currentUserId, courseId, lessonId, command),
                QuizQuestionView::id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Question added successfully", catalogApiMapper.toResponse(view)));
    }

    @Operation(summary = "Update a question (options use merge strategy: idâ†’update, no idâ†’create, missingâ†’delete)",
               security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{questionId}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<QuizQuestionResponse>> updateQuestion(
            @PathVariable String lessonId,
            @PathVariable String questionId,
            @Valid @RequestBody UpdateQuizQuestionRequest request,
            @PathVariable String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Question updated successfully", catalogApiMapper.toResponse(quizQuestionService.updateQuestion(currentUserId, courseId, lessonId, questionId, catalogApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Delete a question (blocked if it would violate numberOfQuestionPerQuizSession)",
               security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> deleteQuestion(
            @PathVariable String lessonId,
            @PathVariable String questionId,
            @PathVariable String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        quizQuestionService.deleteQuestion(currentUserId, courseId, lessonId, questionId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Question deleted successfully", null));
    }
}

