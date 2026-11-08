package com.cinx.commerce.subscription.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A billing provider's webhook event, recorded once per provider, account and event id, with how it was handled. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingWebhookInbox {
    private String provider;
    private String providerAccountKey;
    private String providerEventId;
    private String payloadHash;
    private String providerObjectRef;
    private Long providerObjectVersion;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private String processingStatus;
    private String failureCode;
}
