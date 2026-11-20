package com.cinx.commerce.wishlist.api;

import com.cinx.commerce.wishlist.api.dto.WishlistRequest;
import com.cinx.commerce.wishlist.api.dto.WishlistResponse;
import com.cinx.commerce.wishlist.api.mapper.WishlistMapper;
import com.cinx.commerce.wishlist.application.IWishlistService;
import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasAuthority('PERM_commerce.wishlist.manage.self')")
public class WishlistController {
    private final IWishlistService service;
    private final WishlistMapper mapper;

    @GetMapping
    ResponseEntity<ApiResponse<List<WishlistResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("Wishlist fetched successfully",
                service.findAll(AuthenticationUtil.extractUserId()).stream().map(mapper::toResponse).toList()));
    }

    @PostMapping
    ResponseEntity<ApiResponse<WishlistResponse>> add(@Valid @RequestBody WishlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Course added to wishlist",
                mapper.toResponse(service.add(AuthenticationUtil.extractUserId(), request.courseId()))));
    }

    @DeleteMapping("/{courseId}")
    ResponseEntity<ApiResponse<Void>> remove(@PathVariable String courseId) {
        service.remove(AuthenticationUtil.extractUserId(), courseId);
        return ResponseEntity.ok(ApiResponse.success("Course removed from wishlist", null));
    }
}
