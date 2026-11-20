package com.cinx.learning.instructor.api;

import com.cinx.learning.instructor.api.mapper.InstructorApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.instructor.domain.CommentTemplateKind;
import com.cinx.learning.shared.domain.InboxKind;
import com.cinx.learning.instructor.api.dto.ActivityResponse;
import com.cinx.learning.instructor.api.dto.CommentTemplateDtos;
import com.cinx.learning.instructor.api.dto.InboxResponse;
import com.cinx.learning.instructor.api.dto.OverviewResponse;
import com.cinx.learning.instructor.application.ICommentTemplateService;
import com.cinx.learning.instructor.application.IInstructorActivityService;
import com.cinx.learning.instructor.application.IInstructorInboxService;
import com.cinx.learning.instructor.application.IInstructorOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/** The instructor's own workspace: inbox, overview, the "Đã xử lý" log and saved comments. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/learning/instructor")
@PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
public class InstructorWorkspaceController {
    private final IInstructorInboxService inboxService;
    private final IInstructorOverviewService overviewService;
    private final IInstructorActivityService activityService;
    private final ICommentTemplateService templateService;
    private final InstructorApiMapper instructorApiMapper;

    @Operation(summary = "Waiting work grouped by course and kind", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<InboxResponse>> inbox(
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) InboxKind kind,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success("", instructorApiMapper.toResponse(inboxService.inbox(AuthenticationUtil.extractUserId(), courseId, kind, q))));
    }

    @Operation(summary = "Counters for the studio navigation", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/inbox/summary")
    public ResponseEntity<ApiResponse<InboxResponse.Summary>> summary() {
        return ResponseEntity.ok(ApiResponse.success("", instructorApiMapper.toResponse(inboxService.summary(AuthenticationUtil.extractUserId()))));
    }

    @Operation(summary = "Learners, ratings and waiting work for a period", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<OverviewResponse>> overview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String courseId) {
        return ResponseEntity.ok(ApiResponse.success("", instructorApiMapper.toResponse(overviewService.overview(AuthenticationUtil.extractUserId(), from, to, courseId))));
    }

    @Operation(summary = "The overview's course table as CSV", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping(value = "/overview.csv", produces = "text/csv")
    public ResponseEntity<byte[]> overviewCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String csv = overviewService.overviewCsv(AuthenticationUtil.extractUserId(), from, to);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cinx-overview-" + from + "-" + to + ".csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "What you handled recently", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> activity(
            @RequestParam(required = false) InboxKind kind,
            @RequestParam(required = false) String courseId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.success("", activityService.recent(AuthenticationUtil.extractUserId(), kind, courseId, days).stream().map(instructorApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Your saved comments of one kind", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/comment-templates")
    public ResponseEntity<ApiResponse<List<CommentTemplateDtos.Response>>> templates(@RequestParam CommentTemplateKind kind) {
        return ResponseEntity.ok(ApiResponse.success("", templateService.list(AuthenticationUtil.extractUserId(), kind).stream().map(instructorApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Save a comment to reuse", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/comment-templates")
    public ResponseEntity<ApiResponse<CommentTemplateDtos.Response>> createTemplate(@Valid @RequestBody CommentTemplateDtos.CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Saved", instructorApiMapper.toResponse(templateService.create(AuthenticationUtil.extractUserId(), instructorApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Delete one of your saved comments", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/comment-templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id) {
        templateService.delete(AuthenticationUtil.extractUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
