package com.cinx.commerce.subscription.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_adjustment")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriptionAdjustmentJpaEntity {
    @Id private String id;
    @Column(name = "subscription_id", nullable = false) private String subscriptionId;
    private String featureKey;
    @Lob @Column(columnDefinition = "JSON") private String valueJson;
    @Column(nullable = false, length = 1000) private String reason;
    @Column(nullable = false) private LocalDateTime effectiveFrom;
    private LocalDateTime expiresAt;
    @Column(nullable = false) private String createdBy;
    @Column(nullable = false) private LocalDateTime createdAt;
}
