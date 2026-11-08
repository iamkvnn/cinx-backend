package com.cinx.commerce.order.api;

import com.cinx.commerce.order.api.dto.CheckoutRequest;
import com.cinx.commerce.order.api.dto.OrderResponse;
import com.cinx.commerce.order.api.mapper.OrderMapper;
import com.cinx.commerce.order.application.IOrderService;
import com.cinx.commerce.payment.application.IOrderPaymentService;
import com.cinx.commerce.payment.api.dto.CreatePaymentRequest;
import com.cinx.commerce.payment.api.dto.PaymentLinkResponse;
import com.cinx.commerce.payment.api.mapper.PaymentMapper;
import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/orders") @RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class OrderController {
    private final IOrderService service;
    private final IOrderPaymentService paymentService;
    private final OrderMapper mapper;
    private final PaymentMapper paymentMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_commerce.order.create.self')")
    ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody(required = false) CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Order created successfully",
                mapper.toDto(service.checkout(AuthenticationUtil.extractUserId(), idempotencyKey,
                        request == null ? null : mapper.toCommand(request)))));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAuthority('PERM_commerce.order.create.self')")
    ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully",
                mapper.toDto(service.cancel(AuthenticationUtil.extractUserId(), orderId))));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_commerce.order.read.self')")
    ResponseEntity<ApiResponse<List<OrderResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully",
                service.findAll(AuthenticationUtil.extractUserId()).stream().map(mapper::toDto).toList()));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('PERM_commerce.order.read.self') or hasAuthority('PERM_commerce.order.read.any')")
    ResponseEntity<ApiResponse<OrderResponse>> findById(@PathVariable String orderId,
                                                        Authentication authentication) {
        boolean readAny = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("PERM_commerce.order.read.any"));
        OrderResponse response = mapper.toDto(readAny
                ? service.findByIdAny(orderId)
                : service.findById(AuthenticationUtil.extractUserId(), orderId));
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", response));
    }

    @PostMapping("/{orderId}/payments")
    @PreAuthorize("hasAuthority('PERM_commerce.order.create.self')")
    ResponseEntity<ApiResponse<PaymentLinkResponse>> createPayment(@PathVariable String orderId,
                                                                   @Valid @RequestBody CreatePaymentRequest request,
                                                                   HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success("Payment link created successfully",
                paymentMapper.toResponse(paymentService.createPayment(AuthenticationUtil.extractUserId(), orderId, request.provider(), customerIp(http)))));
    }

    /** The payer's address as the gateway forwarded it; VNPay asks for it and nothing here trusts it. */
    private static String customerIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
