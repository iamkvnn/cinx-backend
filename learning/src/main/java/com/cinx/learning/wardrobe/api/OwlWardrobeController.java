package com.cinx.learning.wardrobe.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.wardrobe.api.dto.EquipOwlSkinsRequest;
import com.cinx.learning.wardrobe.api.dto.OwlWardrobeResponse;
import com.cinx.learning.wardrobe.api.mapper.WardrobeApiMapper;
import com.cinx.learning.wardrobe.application.IOwlWardrobeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The caller's own tutor-owl wardrobe. */
@RestController
@RequestMapping("/api/v1/learning/owl/wardrobe")
@RequiredArgsConstructor
public class OwlWardrobeController {
    private final IOwlWardrobeService service;
    private final WardrobeApiMapper mapper;

    @Operation(summary = "Get my owl wardrobe", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public ResponseEntity<ApiResponse<OwlWardrobeResponse>> getMine() {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Owl wardrobe fetched successfully", mapper.toResponse(service.getWardrobe(userId))));
    }

    @Operation(summary = "Dress my owl (400 when an item is unknown, inactive, of another slot or still locked)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping
    public ResponseEntity<ApiResponse<OwlWardrobeResponse>> equip(@Valid @RequestBody EquipOwlSkinsRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Owl wardrobe updated successfully", mapper.toResponse(service.equip(userId, mapper.toCommand(request)))));
    }
}
