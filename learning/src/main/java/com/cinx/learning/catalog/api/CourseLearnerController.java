package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.response.LearnerSummaryResponse;
import com.cinx.learning.catalog.application.ICourseLearnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/learners")
@RequiredArgsConstructor
public class CourseLearnerController {
    private final ICourseLearnerService learnerService;
    private final CatalogApiMapper catalogApiMapper;

    @Operation(summary = "Names and avatars of the course's participants, for its instructors",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/summaries")
    @PreAuthorize("hasAuthority('PERM_learning.analytics.read.own') or hasAuthority('PERM_learning.analytics.read.any')")
    public ResponseEntity<ApiResponse<List<LearnerSummaryResponse>>> summaries(
            @PathVariable String courseId,
            @RequestParam List<String> ids) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Learner summaries fetched successfully",
                learnerService.summaries(AuthenticationUtil.extractUserId(), courseId, ids).stream().map(catalogApiMapper::toResponse).toList()));
    }
}
