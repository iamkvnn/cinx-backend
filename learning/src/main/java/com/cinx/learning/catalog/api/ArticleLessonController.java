package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.CreateArticleLessonRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateArticleLessonRequest;
import com.cinx.learning.catalog.api.dto.response.ArticleLessonResponse;
import com.cinx.learning.catalog.application.IArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons/{lessonId}/articles")
@RequiredArgsConstructor
public class ArticleLessonController {
    private final IArticleService articleService;
    private final CatalogApiMapper catalogApiMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<ArticleLessonResponse>> getArticleByLessonId(
            @PathVariable String courseId,
            @PathVariable String lessonId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("Success", catalogApiMapper.toResponse(articleService.getReadableArticleByLessonId(currentUserId, courseId, lessonId))));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> createArticleLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @Valid @RequestBody CreateArticleLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        articleService.createArticle(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(ApiResponse.success("Success", null));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> updateArticleLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @Valid @RequestBody UpdateArticleLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        articleService.updateArticle(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(ApiResponse.success("Success", null));
    }
}

