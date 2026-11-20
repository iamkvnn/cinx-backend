package com.cinx.learning.wardrobe.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.wardrobe.api.dto.OwlSkinResponse;
import com.cinx.learning.wardrobe.api.dto.UpdateOwlSkinRequest;
import com.cinx.learning.wardrobe.api.mapper.WardrobeApiMapper;
import com.cinx.learning.wardrobe.application.IOwlWardrobeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin view of the owl skin catalog: switch items on or off and date the seasons. Same permission as featured content. */
@RestController
@RequestMapping("/api/v1/learning/admin/owl-skins")
@RequiredArgsConstructor
public class AdminOwlSkinController {
    private static final String READ = "hasAuthority('PERM_learning.featured.manage.any')";
    private static final String WRITE = "hasAuthority('PERM_learning.featured.manage.any') and @cinxMfaVerifier.requireSatisfied(authentication)";

    private final IOwlWardrobeService service;
    private final WardrobeApiMapper mapper;

    @Operation(summary = "List the owl skin catalog", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    @PreAuthorize(READ)
    public ResponseEntity<ApiResponse<List<OwlSkinResponse>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Owl skins fetched successfully", service.getCatalog().stream().map(mapper::toResponse).toList()));
    }

    @Operation(summary = "Update an owl skin (active flag; season dates for SEASON items)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PatchMapping("/{code}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<OwlSkinResponse>> update(@PathVariable String code, @Valid @RequestBody UpdateOwlSkinRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Owl skin updated successfully", mapper.toResponse(service.updateSkin(code, mapper.toCommand(request)))));
    }
}
