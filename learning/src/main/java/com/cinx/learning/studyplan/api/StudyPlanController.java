package com.cinx.learning.studyplan.api;

import com.cinx.learning.studyplan.api.mapper.StudyplanApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.studyplan.api.dto.request.DayOffRequest;
import com.cinx.learning.studyplan.api.dto.request.UpsertStudyPlanRequest;
import com.cinx.learning.studyplan.api.dto.response.DayOffResponse;
import com.cinx.learning.studyplan.api.dto.response.StudyPlanPreviewResponse;
import com.cinx.learning.studyplan.api.dto.response.StudyPlanResponse;
import com.cinx.learning.studyplan.application.IStudyPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

/** The signed-in learner's own study plan ("Tuần của tôi"); the subject always comes from the JWT. */
@RestController
@RequestMapping("/api/v1/learning/study-plan")
@RequiredArgsConstructor
public class StudyPlanController {
    // the plan is progress data about oneself: the same permissions as the course-progress endpoints
    static final String READ = "hasAuthority('PERM_learning.progress.read.self')";
    static final String WRITE = "hasAuthority('PERM_learning.progress.write.self')";

    private final IStudyPlanService studyPlanService;
    private final StudyplanApiMapper studyplanApiMapper;

    @Operation(summary = "My study plan and one week of it (404 when none)", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    @PreAuthorize(READ)
    public ResponseEntity<ApiResponse<StudyPlanResponse>> get(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Study plan fetched successfully",
                studyplanApiMapper.toResponse(studyPlanService.get(AuthenticationUtil.extractUserId(), weekStart))));
    }

    @Operation(summary = "Create or replace my study plan", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<StudyPlanResponse>> upsert(@Valid @RequestBody UpsertStudyPlanRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Study plan saved successfully",
                studyplanApiMapper.toResponse(studyPlanService.upsert(AuthenticationUtil.extractUserId(), studyplanApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Estimate a finish date for these settings without saving", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/preview")
    @PreAuthorize(READ)
    public ResponseEntity<ApiResponse<StudyPlanPreviewResponse>> preview(@Valid @RequestBody UpsertStudyPlanRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Study plan estimated successfully",
                studyplanApiMapper.toResponse(studyPlanService.preview(AuthenticationUtil.extractUserId(), studyplanApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Mark a day busy; its sessions move to the next free days", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/days-off")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<DayOffResponse>> addDayOff(@Valid @RequestBody DayOffRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Day marked busy",
                studyplanApiMapper.toResponse(studyPlanService.addDayOff(AuthenticationUtil.extractUserId(), request.date()))));
    }

    @Operation(summary = "Undo a busy day", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/days-off/{date}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<StudyPlanResponse>> removeDayOff(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Busy day removed",
                studyplanApiMapper.toResponse(studyPlanService.removeDayOff(AuthenticationUtil.extractUserId(), date))));
    }
}
