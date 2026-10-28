package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.InviteCourseInstructorRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateCourseInstructorRequest;
import com.cinx.learning.catalog.api.dto.response.CourseInstructorMembershipResponse;
import com.cinx.learning.catalog.api.dto.response.InstructorLookupResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.RequestParam;
import com.cinx.learning.catalog.application.ICourseCollaborationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/collaborators")
@RequiredArgsConstructor
public class CourseCollaborationController {
    private final ICourseCollaborationService collaborationService;
    private final CatalogApiMapper catalogApiMapper;

    @Operation(summary = "List course instructors", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<List<CourseInstructorMembershipResponse>>> list(
            @PathVariable String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course collaborators fetched successfully",
                collaborationService.list(AuthenticationUtil.extractUserId(), courseId).stream().map(catalogApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Find the verified instructor behind an exact email (owner only)", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/lookup")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<InstructorLookupResponse>> lookup(
            @PathVariable String courseId,
            @RequestParam @NotBlank @Email @Size(max = 255) String email) {
        return ResponseEntity.ok(ApiResponse.success("Instructor found",
                catalogApiMapper.toResponse(collaborationService.lookup(AuthenticationUtil.extractUserId(), courseId, email))));
    }

    @Operation(summary = "Invite an instructor", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseInstructorMembershipResponse>> invite(
            @PathVariable String courseId,
            @Valid @RequestBody InviteCourseInstructorRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course collaborator invited successfully",
                catalogApiMapper.toResponse(collaborationService.invite(AuthenticationUtil.extractUserId(), courseId, catalogApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Accept course invitation", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/me/accept")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own')")
    public ResponseEntity<ApiResponse<CourseInstructorMembershipResponse>> accept(@PathVariable String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course invitation accepted successfully",
                catalogApiMapper.toResponse(collaborationService.accept(AuthenticationUtil.extractUserId(), courseId))));
    }

    @Operation(summary = "Decline course invitation", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/me/decline")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own')")
    public ResponseEntity<ApiResponse<CourseInstructorMembershipResponse>> decline(@PathVariable String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course invitation declined successfully",
                catalogApiMapper.toResponse(collaborationService.decline(AuthenticationUtil.extractUserId(), courseId))));
    }

    @Operation(summary = "Change collaborator role", security = @SecurityRequirement(name = "bearer-jwt"))
    @PatchMapping("/{instructorId}")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseInstructorMembershipResponse>> updateRole(
            @PathVariable String courseId,
            @PathVariable String instructorId,
            @Valid @RequestBody UpdateCourseInstructorRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Course collaborator role updated successfully",
                catalogApiMapper.toResponse(collaborationService.updateRole(AuthenticationUtil.extractUserId(), courseId, instructorId, catalogApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Revoke collaborator", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{instructorId}")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @PathVariable String courseId,
            @PathVariable String instructorId) {
        collaborationService.revoke(AuthenticationUtil.extractUserId(), courseId, instructorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course collaborator revoked successfully", null));
    }

    @Operation(summary = "Transfer course ownership", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{instructorId}/transfer-ownership")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<Void>> transferOwnership(
            @PathVariable String courseId,
            @PathVariable String instructorId) {
        collaborationService.transferOwnership(AuthenticationUtil.extractUserId(), courseId, instructorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course ownership transferred successfully", null));
    }
}
