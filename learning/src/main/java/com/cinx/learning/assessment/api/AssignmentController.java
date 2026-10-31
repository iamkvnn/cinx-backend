package com.cinx.learning.assessment.api;

import com.cinx.learning.assessment.api.mapper.AssessmentApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.assessment.api.dto.request.CreateAssignmentSubmissionRequest;
import com.cinx.learning.assessment.api.dto.response.AssignmentSubmissionResponse;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import com.cinx.learning.assessment.application.IAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/assessments/assignment-submissions")
public class AssignmentController {
    private final IAssignmentService assignmentService;
    private final LearningAuthorizationService authorizationService;
    private final AssessmentApiMapper assessmentApiMapper;

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
    public ResponseEntity<PaginatedApiResponse<AssignmentSubmissionResponse>> getAssignmentSubmissions(
            @RequestParam String assignmentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        authorizationService.requireLessonInstructor(currentUserId, assignmentId);
        return ResponseEntity.ok(PaginationWrapper.wrap(assignmentService.getAssignmentSubmissions(assignmentId, page, size, sort).map(assessmentApiMapper::toResponse)));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> getAssignmentSubmission(@RequestParam String assignmentId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "", assessmentApiMapper.toResponse(assignmentService.getAssignmentSubmission(userId, assignmentId))));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    public ResponseEntity<ApiResponse<?>> submitAssignment(
            @RequestParam String assignmentId,
            @Valid @RequestBody CreateAssignmentSubmissionRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        assignmentService.submitAssignment(userId, assignmentId, assessmentApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment submitted successfully", null));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{submissionId}/score")
    @PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
    public ResponseEntity<ApiResponse<?>> scoreAssignmentSubmission(
            @PathVariable String submissionId,
            @RequestParam Double score
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        assignmentService.scoreAssignmentSubmission(currentUserId, submissionId, score);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment submission scored successfully", null));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{submissionId}")
    public ResponseEntity<ApiResponse<?>> deleteAssignmentSubmission(@PathVariable String submissionId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        assignmentService.deleteAssignmentSubmission(currentUserId, submissionId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment submission deleted successfully", null));
    }
}
