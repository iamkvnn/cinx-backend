package com.cinx.commerce.catalog.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "plan_product_line")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlanProductLineJpaEntity {
    @Id private String id;
    @Column(nullable = false, unique = true) private String code;
    @Column(name = "customer_segment", nullable = false) private String customerSegment;
    @Column(name = "eligibility_policy_key", nullable = false) private String eligibilityPolicyKey;
    @Column(name = "subscription_slot_code", nullable = false) private String subscriptionSlotCode;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
