package com.cinx.learning.engagement.api;

import com.cinx.learning.studyplan.api.mapper.StudyplanApiMapper;

import com.cinx.learning.engagement.api.mapper.EngagementApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.engagement.api.dto.request.LearningActivityRequest;
import com.cinx.learning.engagement.api.dto.response.LearningActivitySummaryResponse;
import com.cinx.learning.engagement.application.ILearningActivityService;
import com.cinx.learning.studyplan.api.dto.response.LastActivityResponse;
import com.cinx.learning.studyplan.application.IStudyPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learning/activity")
@RequiredArgsConstructor
public class LearningActivityController {
    private final ILearningActivityService learningActivityService;
    private final IStudyPlanService studyPlanService;
    private final EngagementApiMapper engagementApiMapper;
    private final StudyplanApiMapper studyplanApiMapper;

    @Operation(summary = "Record learning activity heartbeat", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> recordActivity(@Valid @RequestBody LearningActivityRequest request) {
        learningActivityService.recordActivity(AuthenticationUtil.extractUserId(), engagementApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Learning activity recorded successfully", null));
    }

    @Operation(summary = "The lesson to resume (\"Học tiếp\"), across all courses or in one course; 404 before any activity", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/last")
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.self')")
    public ResponseEntity<ApiResponse<LastActivityResponse>> last(@RequestParam(required = false) String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Last activity fetched successfully",
                studyplanApiMapper.toResponse(studyPlanService.lastActivity(AuthenticationUtil.extractUserId(), courseId))));
    }

    @Operation(summary = "My study time: total, and each day of the current week (Monday first)", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.self')")
    public ResponseEntity<ApiResponse<LearningActivitySummaryResponse>> summary() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Learning activity summary fetched successfully",
                engagementApiMapper.toResponse(learningActivityService.getMyActivitySummary(AuthenticationUtil.extractUserId()))));
    }
}
