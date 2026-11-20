package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PresignedUrlResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.CreateSubtitleTrackRequest;
import com.cinx.learning.catalog.api.dto.request.GenerateDefaultSubtitleJobRequest;
import com.cinx.learning.catalog.api.dto.request.TranslateSubtitleJobRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateSubtitleContentRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateSubtitleTrackRequest;
import com.cinx.learning.catalog.api.dto.response.SubtitleContentResponse;
import com.cinx.learning.catalog.api.dto.response.SubtitleJobResponse;
import com.cinx.learning.catalog.api.dto.response.SubtitleTrackResponse;
import com.cinx.learning.catalog.api.dto.response.SubtitleWordConfidenceResponse;
import com.cinx.learning.catalog.application.ISubtitleJobService;
import com.cinx.learning.catalog.application.ISubtitleTrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons/{lessonId}/videos/subtitles")
@RequiredArgsConstructor
public class SubtitleTrackController {
    private final ISubtitleTrackService subtitleTrackService;
    private final ISubtitleJobService subtitleJobService;
    private final CatalogApiMapper catalogApiMapper;

    @GetMapping
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<SubtitleTrackResponse>>> getSubtitles(
            @PathVariable String courseId,
            @PathVariable String lessonId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitles fetched successfully",
                subtitleTrackService.getSubtitlesByLessonId(currentUserId, courseId, lessonId).stream().map(catalogApiMapper::toResponse).toList()
        ));
    }

    @GetMapping("/presigned-url")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getSubtitlePresignedUrl(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam String languageCode
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitle upload URL generated successfully",
                subtitleTrackService.getSubtitlePresignedUrl(currentUserId, courseId, lessonId, fileName, contentType, languageCode)
        ));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<SubtitleTrackResponse>> createSubtitle(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestBody @Valid CreateSubtitleTrackRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitle created successfully",
                catalogApiMapper.toResponse(subtitleTrackService.createSubtitle(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request)))
        ));
    }

    @PostMapping("/ai/default")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<SubtitleJobResponse>> createDefaultSubtitleJob(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestBody @Valid GenerateDefaultSubtitleJobRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Default subtitle generation job created successfully",
                catalogApiMapper.toResponse(subtitleJobService.createDefaultSubtitleJob(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request)))
        ));
    }

    @PostMapping("/ai/translations")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<SubtitleJobResponse>>> createTranslationJobs(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestBody @Valid TranslateSubtitleJobRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitle translation jobs created successfully",
                subtitleJobService.createTranslationJobs(currentUserId, courseId, lessonId, catalogApiMapper.toCommand(request)).stream().map(catalogApiMapper::toResponse).toList()
        ));
    }

    @GetMapping("/jobs")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<SubtitleJobResponse>>> getSubtitleJobs(
            @PathVariable String courseId,
            @PathVariable String lessonId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitle jobs fetched successfully",
                subtitleJobService.getJobsByLessonId(currentUserId, courseId, lessonId).stream().map(catalogApiMapper::toResponse).toList()
        ));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<SubtitleJobResponse>> getSubtitleJob(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @PathVariable String jobId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitle job fetched successfully",
                catalogApiMapper.toResponse(subtitleJobService.getJobById(currentUserId, courseId, lessonId, jobId))
        ));
    }

    @PutMapping("/{subtitleId}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<SubtitleTrackResponse>> updateSubtitle(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @PathVariable String subtitleId,
            @RequestBody @Valid UpdateSubtitleTrackRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitle updated successfully",
                catalogApiMapper.toResponse(subtitleTrackService.updateSubtitle(currentUserId, courseId, lessonId, subtitleId, catalogApiMapper.toCommand(request)))
        ));
    }

    @GetMapping("/{subtitleId}/content")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<SubtitleContentResponse>> getSubtitleContent(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @PathVariable String subtitleId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitle content fetched successfully",
                catalogApiMapper.toResponse(subtitleTrackService.getSubtitleContent(currentUserId, courseId, lessonId, subtitleId))
        ));
    }

    @PutMapping("/{subtitleId}/content")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<SubtitleTrackResponse>> updateSubtitleContent(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @PathVariable String subtitleId,
            @RequestBody @Valid UpdateSubtitleContentRequest request
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitle content updated successfully",
                catalogApiMapper.toResponse(subtitleTrackService.updateSubtitleContent(currentUserId, courseId, lessonId, subtitleId, catalogApiMapper.toCommand(request)))
        ));
    }

    @PostMapping("/{subtitleId}/approve")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<SubtitleTrackResponse>> approveSubtitle(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @PathVariable String subtitleId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitle approved successfully",
                catalogApiMapper.toResponse(subtitleTrackService.approveSubtitle(currentUserId, courseId, lessonId, subtitleId))
        ));
    }

    @GetMapping("/{subtitleId}/word-confidence")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<SubtitleWordConfidenceResponse>> getSubtitleWordConfidence(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @PathVariable String subtitleId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Subtitle word confidence fetched successfully",
                catalogApiMapper.toResponse(subtitleTrackService.getSubtitleWordConfidence(currentUserId, courseId, lessonId, subtitleId))
        ));
    }

    @DeleteMapping("/{subtitleId}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> deleteSubtitle(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @PathVariable String subtitleId
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        subtitleTrackService.deleteSubtitle(currentUserId, courseId, lessonId, subtitleId);
        return ResponseEntity.ok(ApiResponse.success("Subtitle deleted successfully", null));
    }
}

