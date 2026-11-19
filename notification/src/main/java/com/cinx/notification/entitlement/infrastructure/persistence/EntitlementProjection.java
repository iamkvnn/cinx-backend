package com.cinx.notification.entitlement.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_entitlement_projection")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EntitlementProjection {
    @Id private String userId;
    @Column(nullable = false) private long entitlementVersion;
    @Column(nullable = false, length = 32) private String accessStatus;
    private LocalDateTime validUntil;
    private LocalDateTime freshnessLeaseUntil;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
