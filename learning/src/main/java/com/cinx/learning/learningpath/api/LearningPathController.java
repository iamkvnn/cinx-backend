package com.cinx.learning.learningpath.api;

import com.cinx.learning.learningpath.api.mapper.LearningpathApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.learningpath.api.dto.request.LearningPathItemsUpdateRequest;
import com.cinx.learning.learningpath.api.dto.request.LearningPathRequest;
import com.cinx.learning.learningpath.api.dto.response.LearningPathResponse;
import com.cinx.learning.learningpath.application.ILearningPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/learning/paths")
@RequiredArgsConstructor
public class LearningPathController {
    
    private final ILearningPathService learningPathService;
    private final LearningpathApiMapper learningpathApiMapper;

    @Operation(summary = "Get current user's learning paths", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public ResponseEntity<ApiResponse<List<LearningPathResponse>>> getLearningPaths() {
        String userId = AuthenticationUtil.extractUserId();
        List<LearningPathResponse> response = learningPathService.getLearningPaths(userId).stream().map(learningpathApiMapper::toResponse).toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Learning paths retrieved successfully", response));
    }

    @Operation(summary = "Get current user's learning path", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LearningPathResponse>> getLearningPath(@PathVariable String id) {
        String userId = AuthenticationUtil.extractUserId();
        LearningPathResponse response = learningpathApiMapper.toResponse(learningPathService.getLearningPath(userId, id));
        return ResponseEntity.ok(new ApiResponse<>(true, "Learning path retrieved successfully", response));
    }

    @Operation(summary = "Create current user's learning path", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    public ResponseEntity<ApiResponse<LearningPathResponse>> createLearningPath(
            @Parameter(description = "Optional. Repeating a key with the same request returns the path created the first time.")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody LearningPathRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        LearningPathResponse response = learningpathApiMapper.toResponse(learningPathService.createLearningPath(userId, idempotencyKey, learningpathApiMapper.toCommand(request)));
        return ResponseEntity.ok(new ApiResponse<>(true, "Learning path created successfully", response));
    }

    @Operation(summary = "Replace the lessons of the current user's active or pending learning path",
            description = "Progress and status are recomputed. A course new to the path that the user does not own goes to the cart and the path becomes PENDING_PAYMENT. Repeating the same request changes nothing.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{id}/items")
    public ResponseEntity<ApiResponse<LearningPathResponse>> replaceItems(
            @PathVariable String id, @Valid @RequestBody LearningPathItemsUpdateRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        LearningPathResponse response = learningpathApiMapper.toResponse(learningPathService.replaceItems(userId, id, learningpathApiMapper.toCommand(request)));
        return ResponseEntity.ok(new ApiResponse<>(true, "Learning path updated successfully", response));
    }

    @Operation(summary = "Get current user's active learning path", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<LearningPathResponse>> getActiveLearningPath() {
        String userId = AuthenticationUtil.extractUserId();
        LearningPathResponse response = learningpathApiMapper.toResponse(learningPathService.getActiveLearningPath(userId));
        return ResponseEntity.ok(new ApiResponse<>(true, "Active learning path retrieved", response));
    }

    @Operation(summary = "Drop current user's active learning path", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/active")
    public ResponseEntity<ApiResponse<Void>> dropActiveLearningPath() {
        String userId = AuthenticationUtil.extractUserId();
        learningPathService.dropActiveLearningPath(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Learning path dropped successfully", null));
    }
}
