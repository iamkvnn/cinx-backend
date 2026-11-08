package com.cinx.commerce.catalog.domain;

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
public class PlanProductLine {
    private String id;
    private String code;
    private String customerSegment;
    private String eligibilityPolicyKey;
    private String subscriptionSlotCode;
    private int displayOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
