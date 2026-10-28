package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.CreateVideoQuestionRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateVideoQuestionRequest;
import com.cinx.learning.catalog.api.dto.response.VideoQuestionResponse;
import com.cinx.learning.catalog.application.IVideoQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons/{lessonId}/videos/questions")
@RequiredArgsConstructor
public class VideoQuestionController {

    private final IVideoQuestionService videoQuestionService;
    private final CatalogApiMapper catalogApiMapper;

    @GetMapping
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<VideoQuestionResponse>>> getQuestionsByLessonId(
            @PathVariable String courseId,
            @PathVariable String lessonId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Questions fetched successfully",
                videoQuestionService.getQuestionsByLessonId(currentUserId, courseId, lessonId).stream().map(catalogApiMapper::toResponse).toList()));
    }

    @GetMapping("/{id}")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<VideoQuestionResponse>> getQuestionById(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @PathVariable String id
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity
                .ok(new ApiResponse<>(true, "Question fetched successfully", catalogApiMapper.toResponse(videoQuestionService.getQuestionById(currentUserId, courseId, lessonId, id))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<VideoQuestionResponse>> createQuestion(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestBody @Valid CreateVideoQuestionRequest request) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Video question created",
                catalogApiMapper.toResponse(videoQuestionService.createQuestion(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request)))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<VideoQuestionResponse>> updateQuestion(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @PathVariable String id,
            @RequestBody @Valid UpdateVideoQuestionRequest request) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Video question updated", catalogApiMapper.toResponse(videoQuestionService.updateQuestion(currentUserId, courseId, lessonId, id, catalogApiMapper.toCommand(request)))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @PathVariable String id
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        videoQuestionService.deleteQuestion(currentUserId, courseId, lessonId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Video question deleted", null));
    }
}

