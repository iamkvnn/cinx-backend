package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.api.dto.request.CreateCategoryRequest;
import com.cinx.learning.catalog.api.dto.request.UpdateCategoryRequest;
import com.cinx.learning.catalog.api.dto.response.CategorySummaryResponse;
import com.cinx.learning.catalog.application.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final ICategoryService categoryService;
    private final CatalogApiMapper catalogApiMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategorySummaryResponse>>> getAllCategories() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Success", categoryService.getAllCategories().stream().map(catalogApiMapper::toResponse).toList())
        );
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_learning.course.subscription-policy.manage.any')")
    public ResponseEntity<ApiResponse<?>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        categoryService.createCategory(catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Success", null)
        );
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_learning.course.subscription-policy.manage.any')")
    public ResponseEntity<ApiResponse<?>> updateCategory(@PathVariable String id, @Valid @RequestBody UpdateCategoryRequest request) {
        categoryService.updateCategory(id, catalogApiMapper.toCommand(request));
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Success", null)
        );
    }
}

