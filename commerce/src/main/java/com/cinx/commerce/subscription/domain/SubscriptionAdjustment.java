package com.cinx.commerce.subscription.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionAdjustment {
    private String id;
    private String subscriptionId;
    private String featureKey;
    private String valueJson;
    private String reason;
    private LocalDateTime effectiveFrom;
    private LocalDateTime expiresAt;
    private String createdBy;
    private LocalDateTime createdAt;
}
