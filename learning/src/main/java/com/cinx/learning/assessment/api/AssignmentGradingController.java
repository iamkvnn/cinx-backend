package com.cinx.learning.assessment.api;

import com.cinx.learning.assessment.api.mapper.AssessmentApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.assessment.api.dto.request.GradeSubmissionRequest;
import com.cinx.learning.assessment.api.dto.request.LineCommentRequest;
import com.cinx.learning.assessment.api.dto.request.SaveCriteriaRequest;
import com.cinx.learning.assessment.api.dto.response.AssignmentGradingResponse;
import com.cinx.learning.assessment.application.IAssignmentGradingService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The inbox grader: one assignment's criteria and submissions, grading with criteria, line comments, file text. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/assessments")
@PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
public class AssignmentGradingController {
    private final IAssignmentGradingService gradingService;
    private final AssessmentApiMapper assessmentApiMapper;

    @Operation(summary = "Criteria and submissions of an assignment, ungraded first", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/assignments/{lessonId}/grading")
    public ResponseEntity<ApiResponse<AssignmentGradingResponse>> getGrading(@PathVariable String lessonId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("", assessmentApiMapper.toResponse(gradingService.getGrading(userId, lessonId))));
    }

    @Operation(summary = "Replace an assignment's criteria (they must add up to 10)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/assignments/{lessonId}/criteria")
    public ResponseEntity<ApiResponse<List<AssignmentGradingResponse.Criterion>>> saveCriteria(
            @PathVariable String lessonId,
            @Valid @RequestBody SaveCriteriaRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("Criteria saved", gradingService.saveCriteria(userId, lessonId, assessmentApiMapper.toCommand(request)).stream().map(assessmentApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Grade a submission with criteria, adjustments and feedback", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/assignment-submissions/{submissionId}/grade")
    public ResponseEntity<ApiResponse<AssignmentGradingResponse.Submission>> grade(
            @PathVariable String submissionId,
            @Valid @RequestBody GradeSubmissionRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("Submission graded", assessmentApiMapper.toResponse(gradingService.grade(userId, submissionId, assessmentApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Comment on one line of a submitted file", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/assignment-submissions/{submissionId}/comments")
    public ResponseEntity<ApiResponse<AssignmentGradingResponse.LineComment>> addComment(
            @PathVariable String submissionId,
            @Valid @RequestBody LineCommentRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("Comment added", assessmentApiMapper.toResponse(gradingService.addComment(userId, submissionId, assessmentApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Delete one of your line comments", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/assignment-submissions/{submissionId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable String submissionId, @PathVariable String commentId) {
        String userId = AuthenticationUtil.extractUserId();
        gradingService.deleteComment(userId, submissionId, commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "The text of a submitted text file (up to 512 KB)", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/assignment-submissions/{submissionId}/attachments/{attachmentId}/text")
    public ResponseEntity<ApiResponse<AssignmentGradingResponse.AttachmentText>> attachmentText(
            @PathVariable String submissionId,
            @PathVariable String attachmentId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("", assessmentApiMapper.toResponse(gradingService.readAttachmentText(userId, submissionId, attachmentId))));
    }
}
