package com.cinx.commerce.subscription.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_webhook_inbox")
@IdClass(BillingWebhookInboxJpaEntity.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class BillingWebhookInboxJpaEntity {
    @Id
    @Column(nullable = false, length = 40)
    private String provider;
    @Id
    @Column(name = "provider_account_key", nullable = false, length = 100)
    private String providerAccountKey;
    @Id
    @Column(name = "provider_event_id", nullable = false, length = 255)
    private String providerEventId;
    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;
    @Column(name = "provider_object_ref")
    private String providerObjectRef;
    @Column(name = "provider_object_version")
    private Long providerObjectVersion;
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    @Column(name = "processing_status", nullable = false, length = 30)
    private String processingStatus;
    private String failureCode;

    public record Key(String provider, String providerAccountKey, String providerEventId) implements Serializable {
    }
}
