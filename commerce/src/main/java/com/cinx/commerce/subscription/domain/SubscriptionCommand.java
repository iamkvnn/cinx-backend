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
public class SubscriptionCommand {
    private String commandId;
    private String subjectId;
    private String subscriptionSlotCode;
    private String idempotencyKey;
    private String requestFingerprint;
    private String commandType;
    private String providerIdempotencyKey;
    private String responseReference;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
