package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.application.ICourseImageService;
import com.cinx.learning.catalog.api.dto.request.CreateCourseImageRequest;
import com.cinx.learning.catalog.api.dto.request.ReorderCourseImagesRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateCourseImageRequest;
import com.cinx.learning.catalog.api.dto.response.CourseImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseImageController {
    private final ICourseImageService courseImageService;
    private final CatalogApiMapper catalogApiMapper;

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{courseId}/images")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> uploadCourseImages(@PathVariable String courseId, @RequestBody CreateCourseImageRequest request) {
        courseImageService.saveCourseImages(com.cinx.common.utils.AuthenticationUtil.extractUserId(), courseId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Course images added successfully", null));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{courseId}/images/{imageId}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> updateCourseImage(@PathVariable String imageId, @RequestBody UpdateCourseImageRequest request, @PathVariable String courseId) {
        courseImageService.updateCourseImage(com.cinx.common.utils.AuthenticationUtil.extractUserId(), courseId, imageId, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Course image updated successfully", null));
    }

    @Operation(summary = "Reorder course images; the first becomes the cover", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{courseId}/images/order")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<List<CourseImageResponse>>> reorderCourseImages(@PathVariable String courseId, @Valid @RequestBody ReorderCourseImagesRequest request) {
        List<CourseImageResponse> images = courseImageService.reorderImages(com.cinx.common.utils.AuthenticationUtil.extractUserId(), courseId, catalogApiMapper.toCommand(request)).stream().map(catalogApiMapper::toResponse).toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Course images reordered successfully", images));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{courseId}/images/{imageId}")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<?>> deleteCourseImage(@PathVariable String imageId, @PathVariable String courseId) {
        courseImageService.deleteImage(com.cinx.common.utils.AuthenticationUtil.extractUserId(), courseId, imageId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course image deleted successfully", null));
    }
}

