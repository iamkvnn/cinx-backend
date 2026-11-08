package com.cinx.commerce.subscription.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_command", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription_command_key", columnNames = {"subject_id", "idempotency_key"}),
        @UniqueConstraint(name = "uk_subscription_provider_command", columnNames = "provider_idempotency_key")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriptionCommandJpaEntity {
    @Id private String commandId;
    @Column(nullable = false) private String subjectId;
    @Column(nullable = false) private String subscriptionSlotCode;
    @Column(nullable = false) private String idempotencyKey;
    @Column(nullable = false) private String requestFingerprint;
    @Column(nullable = false) private String commandType;
    @Column(nullable = false) private String providerIdempotencyKey;
    private String responseReference;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
