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
public class SubscriptionPlan {
    private String id;
    private String productLineId;
    private String code;
    private String lifecycleStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
