package com.cinx.commerce.catalog.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plan")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriptionPlanJpaEntity {
    @Id private String id;
    @Column(name = "product_line_id", nullable = false) private String productLineId;
    @Column(nullable = false, unique = true) private String code;
    @Column(name = "lifecycle_status", nullable = false) private String lifecycleStatus;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
