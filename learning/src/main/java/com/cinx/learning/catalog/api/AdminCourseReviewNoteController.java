package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.api.dto.request.ReviewNoteRequest;
import com.cinx.learning.catalog.api.dto.response.ReviewNoteResponse;
import com.cinx.learning.catalog.application.ICourseReviewNoteService;
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

/** A reviewer's draft notes on a course under review; they are sent with the rejection. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/courses/{id}/review-notes")
@PreAuthorize("hasAuthority('PERM_learning.course.review.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
public class AdminCourseReviewNoteController {
    private final ICourseReviewNoteService reviewNoteService;
    private final CatalogApiMapper catalogApiMapper;

    @Operation(summary = "Draft review notes of a course", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewNoteResponse>>> drafts(@PathVariable("id") String courseId) {
        return ResponseEntity.ok(ApiResponse.success("", reviewNoteService.drafts(courseId).stream().map(catalogApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Add a draft review note", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewNoteResponse>> create(@PathVariable("id") String courseId,
                                                                  @Valid @RequestBody ReviewNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Note added", catalogApiMapper.toResponse(reviewNoteService.create(courseId, catalogApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Edit a draft review note", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{noteId}")
    public ResponseEntity<ApiResponse<ReviewNoteResponse>> update(@PathVariable("id") String courseId,
                                                                  @PathVariable String noteId,
                                                                  @Valid @RequestBody ReviewNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Note updated", catalogApiMapper.toResponse(reviewNoteService.update(courseId, noteId, catalogApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Delete a draft review note", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@PathVariable("id") String courseId, @PathVariable String noteId) {
        reviewNoteService.delete(courseId, noteId);
        return ResponseEntity.noContent().build();
    }
}
