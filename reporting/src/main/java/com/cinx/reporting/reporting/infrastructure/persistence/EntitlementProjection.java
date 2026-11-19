package com.cinx.reporting.reporting.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reporting_entitlement_projection")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EntitlementProjection {
    @Id private String userId;
    @Column(nullable = false) private long entitlementVersion;
    @Column(nullable = false, length = 32) private String accessStatus;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
