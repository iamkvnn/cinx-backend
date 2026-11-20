package com.cinx.commerce.subscription.api;

import com.cinx.commerce.subscription.application.SubscriptionService;

import com.cinx.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/entitlements")
public class InternalEntitlementController {
    private final SubscriptionService service;

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('PERM_commerce.internal.entitlement.read') and @cinxServiceClient.is(authentication, 'cinx-learning-service', 'cinx-recommendation-service', 'cinx-assistant-service')")
    public ApiResponse<Map<String, Object>> get(@PathVariable String userId) {
        return ApiResponse.success("Entitlement snapshot fetched successfully", service.entitlementSnapshot(userId));
    }
}
