package com.cinx.learning.entitlement.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_user_entitlement_projection")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserEntitlementProjection {
    @Id private String userId;
    @Column(nullable = false) private String subjectId;
    @Lob @Column(nullable = false, columnDefinition = "JSON") private String sourceSummaryJson;
    @Column(nullable = false) private long entitlementVersion;
    @Column(nullable = false) private String accessStatus;
    @Column(nullable = false) private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil;
    private LocalDateTime reconcileBy;
    @Column(nullable = false) private String snapshotHash;
    @Version @Column(nullable = false) private long optimisticVersion;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
