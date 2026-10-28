package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.request.UpdateCoursePromoVideoRequest;
import com.cinx.learning.catalog.api.dto.response.CourseResponse;
import com.cinx.learning.catalog.application.ICoursePromoVideoService;
import com.cinx.learning.catalog.application.ICourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CoursePromoVideoController {
    private final ICoursePromoVideoService promoVideoService;
    private final ICourseService courseService;
    private final CatalogApiMapper catalogApiMapper;

    @Operation(summary = "Set or replace the course intro video", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{courseId}/promo-video")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseResponse>> putPromoVideo(@PathVariable String courseId, @Valid @RequestBody UpdateCoursePromoVideoRequest request) {
        String currentUserId = AuthenticationUtil.extractUserId();
        promoVideoService.putPromoVideo(currentUserId, courseId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Course intro video saved successfully",
                catalogApiMapper.toResponse(courseService.getEditableDraftCourseById(currentUserId, courseId))));
    }

    @Operation(summary = "Remove the course intro video", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{courseId}/promo-video")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<CourseResponse>> removePromoVideo(@PathVariable String courseId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        promoVideoService.removePromoVideo(currentUserId, courseId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course intro video removed successfully",
                catalogApiMapper.toResponse(courseService.getEditableDraftCourseById(currentUserId, courseId))));
    }
}
