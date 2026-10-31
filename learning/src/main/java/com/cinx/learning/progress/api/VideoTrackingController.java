package com.cinx.learning.progress.api;

import com.cinx.learning.progress.api.mapper.ProgressApiMapper;

import com.cinx.learning.assessment.api.mapper.AssessmentApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.assessment.api.dto.request.SubmitVideoQuestionRequest;
import com.cinx.learning.progress.api.dto.request.TrackingVideoLessonRequest;
import com.cinx.learning.assessment.api.dto.response.InVideoAssessmentSubmissionResponse;
import com.cinx.learning.progress.api.dto.response.VideoLessonTrackingHistoryResponse;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import com.cinx.learning.progress.application.IVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/learning")
public class VideoTrackingController {
    private final IVideoService videoService;
    private final LearningAuthorizationService authorizationService;
    private final AssessmentApiMapper assessmentApiMapper;
    private final ProgressApiMapper progressApiMapper;

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/courses/{courseId}/lessons/{lessonId}/video-tracking")
    @PreAuthorize("hasAuthority('PERM_learning.analytics.read.own')")
    public ResponseEntity<PaginatedApiResponse<VideoLessonTrackingHistoryResponse>> getVideoLessonTrackingHistories(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        String currentUserId = AuthenticationUtil.extractUserId();
        authorizationService.requireLessonInstructor(currentUserId, lessonId);
        return ResponseEntity.ok(PaginationWrapper.wrap(videoService.getVideoLessonTrackingHistories(courseId, lessonId, page, size, sort).map(progressApiMapper::toResponse)));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/courses/{courseId}/lessons/{lessonId}/video-tracking/history")
    public ResponseEntity<ApiResponse<VideoLessonTrackingHistoryResponse>> getVideoLessonTrackingHistory(
            @PathVariable String courseId,
            @PathVariable String lessonId
    ) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "", progressApiMapper.toResponse(videoService.getVideoLessonTrackingHistory(courseId, userId, lessonId))));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/courses/{courseId}/lessons/{lessonId}/video-tracking/submissions")
    public ResponseEntity<ApiResponse<List<InVideoAssessmentSubmissionResponse>>> getVideoQuestionSubmissions(
            @PathVariable String courseId,
            @PathVariable String lessonId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Submissions fetched successfully", videoService.getVideoQuestionSubmissions(courseId, userId, lessonId).stream().map(assessmentApiMapper::toResponse).toList()));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/courses/{courseId}/lessons/{lessonId}/video-tracking")
    public ResponseEntity<ApiResponse<?>> trackVideoProgress(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @Valid @RequestBody TrackingVideoLessonRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        videoService.trackVideoProgress(courseId, lessonId, userId, progressApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Video progress tracked successfully", null));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/courses/{courseId}/lessons/{lessonId}/video-tracking/questions/submit")
    public ResponseEntity<ApiResponse<?>> submitVideoQuestionAnswer(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @Valid @RequestBody SubmitVideoQuestionRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        videoService.submitVideoQuestionAnswer(courseId, lessonId, userId, assessmentApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Answer submitted successfully", null));
    }
}
