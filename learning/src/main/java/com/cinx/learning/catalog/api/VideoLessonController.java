package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.CreateVideoLessonRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateVideoLessonRequest;
import com.cinx.learning.catalog.api.dto.response.VideoLessonResponse;
import com.cinx.learning.catalog.application.IVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons/{lessonId}/videos")
@RequiredArgsConstructor
public class VideoLessonController {
    private final IVideoService videoService;
    private final CatalogApiMapper catalogApiMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<VideoLessonResponse>> getVideoByLessonId(
            @PathVariable String courseId,
            @PathVariable String lessonId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Success", catalogApiMapper.toResponse(videoService.getReadableVideoByLessonId(currentUserId, courseId, lessonId)))
        );
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> createVideoLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestBody CreateVideoLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        videoService.createVideo(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Success", null)
        );
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> updateVideoLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestBody UpdateVideoLessonRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        videoService.updateVideo(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Success", null)
        );
    }
}

