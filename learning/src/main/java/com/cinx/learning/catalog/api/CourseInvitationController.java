package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.response.InvitationResponse;
import com.cinx.learning.catalog.application.ICourseCollaborationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The caller's own pending co-instructor invitations; accept/decline stay on the course's collaborators. */
@RestController
@RequestMapping("/api/v1/courses/collaborators/invitations")
@RequiredArgsConstructor
public class CourseInvitationController {
    private final ICourseCollaborationService collaborationService;
    private final CatalogApiMapper catalogApiMapper;

    @Operation(summary = "My pending course invitations", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own')")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> myInvitations() {
        return ResponseEntity.ok(ApiResponse.success("Invitations fetched successfully",
                collaborationService.myInvitations(AuthenticationUtil.extractUserId()).stream().map(catalogApiMapper::toResponse).toList()));
    }
}
