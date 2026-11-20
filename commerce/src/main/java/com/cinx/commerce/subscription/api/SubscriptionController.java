package com.cinx.commerce.subscription.api;

import com.cinx.commerce.subscription.api.mapper.SubscriptionMapper;
import com.cinx.commerce.subscription.application.SubscriptionService;

import com.cinx.commerce.subscription.api.dto.CheckoutSessionRequest;
import com.cinx.commerce.subscription.api.dto.CheckoutSessionResponse;
import com.cinx.commerce.subscription.api.dto.SubscriptionResponse;
import com.cinx.commerce.subscription.api.dto.SubscriptionChangeRequest;
import com.cinx.commerce.subscription.api.dto.SubscriptionAdjustmentRequest;
import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@RequestMapping("/api/v1")
public class SubscriptionController {
    private final SubscriptionService service;
    private final SubscriptionMapper mapper;

    @GetMapping("/subscriptions/me")
    @PreAuthorize("hasAuthority('PERM_commerce.subscription.read.self')")
    ResponseEntity<ApiResponse<SubscriptionResponse>> read() {
        return ResponseEntity.ok(ApiResponse.success("Subscription fetched successfully",
                mapper.toResponse(service.read(AuthenticationUtil.extractUserId()))));
    }

    @GetMapping("/entitlements/me")
    @PreAuthorize("hasAuthority('PERM_commerce.subscription.read.self')")
    ResponseEntity<ApiResponse<Map<String, Object>>> entitlements() {
        return ResponseEntity.ok(ApiResponse.success("Entitlements fetched successfully",
                service.entitlementSnapshot(AuthenticationUtil.extractUserId())));
    }

    @PostMapping("/subscriptions/checkout-sessions")
    @PreAuthorize("hasAuthority('PERM_commerce.subscription.change.self')")
    ResponseEntity<ApiResponse<CheckoutSessionResponse>> checkout(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody CheckoutSessionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Checkout session created successfully",
                mapper.toResponse(service.createCheckout(AuthenticationUtil.extractUserId(), key, request.offerId(), request.returnPath()))));
    }

    @PostMapping("/subscriptions/me/cancel")
    @PreAuthorize("hasAuthority('PERM_commerce.subscription.change.self')")
    ResponseEntity<ApiResponse<Void>> cancel(@RequestHeader("Idempotency-Key") String key) {
        service.cancel(AuthenticationUtil.extractUserId(), key);
        return ResponseEntity.ok(ApiResponse.success("Subscription cancellation scheduled", null));
    }

    @PostMapping("/subscriptions/me/change")
    @PreAuthorize("hasAuthority('PERM_commerce.subscription.change.self')")
    ResponseEntity<ApiResponse<SubscriptionResponse>> change(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody SubscriptionChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subscription change accepted",
                mapper.toResponse(service.change(AuthenticationUtil.extractUserId(), key, request.offerId()))));
    }

    @PostMapping("/subscriptions/me/resume")
    @PreAuthorize("hasAuthority('PERM_commerce.subscription.change.self')")
    ResponseEntity<ApiResponse<Void>> resume(@RequestHeader("Idempotency-Key") String key) {
        service.resume(AuthenticationUtil.extractUserId(), key);
        return ResponseEntity.ok(ApiResponse.success("Subscription resumed", null));
    }

    @PostMapping("/admin/subscriptions/{subscriptionId}/adjust")
    @PreAuthorize("hasAuthority('PERM_commerce.subscription.adjust.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    ResponseEntity<ApiResponse<SubscriptionResponse>> adjust(
            @PathVariable String subscriptionId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SubscriptionAdjustmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subscription adjustment applied",
                mapper.toResponse(service.adjust(subscriptionId, idempotencyKey, mapper.toCommand(request), AuthenticationUtil.extractUserId()))));
    }
}
