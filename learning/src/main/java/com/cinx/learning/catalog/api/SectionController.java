package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.CreateSectionRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateSectionRequest;
import com.cinx.learning.catalog.api.dto.response.SectionResponse;
import com.cinx.learning.catalog.application.ISectionService;
import com.cinx.learning.catalog.application.IdempotentWrites;
import com.cinx.learning.catalog.application.query.SectionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/sections")
@RequiredArgsConstructor
public class SectionController {
    private final ISectionService sectionService;
    private final CatalogApiMapper catalogApiMapper;
    private final IdempotentWrites idempotentWrites;

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<SectionResponse>> createSection(
            @PathVariable String courseId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateSectionRequest request
    ) {
        String userId = AuthenticationUtil.extractUserId();
        var command = catalogApiMapper.toCommand(request);
        SectionView view = idempotentWrites.once(userId, idempotencyKey, "section.create:" + courseId, command, SectionView.class,
                () -> sectionService.createSection(userId, courseId, command), SectionView::id);
        return ResponseEntity.ok(ApiResponse.success("Section created successfully", catalogApiMapper.toResponse(view)));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{sectionId}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<SectionResponse>> updateSection(
            @PathVariable String courseId,
            @PathVariable String sectionId,
            @RequestBody UpdateSectionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Section updated successfully",
                catalogApiMapper.toResponse(sectionService.updateSection(AuthenticationUtil.extractUserId(), courseId, sectionId, catalogApiMapper.toCommand(request)))
        ));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{sectionId}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable String courseId,
            @PathVariable String sectionId
    ) {
        sectionService.deleteSection(AuthenticationUtil.extractUserId(), courseId, sectionId);
        return ResponseEntity.ok(ApiResponse.success("Section deleted successfully", null));
    }
}

