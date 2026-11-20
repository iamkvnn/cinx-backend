package com.cinx.commerce.cart.api;

import com.cinx.commerce.cart.api.dto.AddToCartRequest;
import com.cinx.commerce.cart.api.dto.CartItemResponse;
import com.cinx.commerce.cart.api.mapper.CartMapper;
import com.cinx.commerce.cart.application.ICartService;
import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasAuthority('PERM_commerce.cart.manage.self')")
public class CartController {
    private final ICartService service;
    private final CartMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully",
                service.findAll(AuthenticationUtil.extractUserId()).stream().map(mapper::toResponse).toList()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemResponse>> add(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Course added to cart",
                mapper.toResponse(service.add(AuthenticationUtil.extractUserId(), request.courseId()))));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable String courseId) {
        service.remove(AuthenticationUtil.extractUserId(), courseId);
        return ResponseEntity.ok(ApiResponse.success("Course removed from cart", null));
    }
}
