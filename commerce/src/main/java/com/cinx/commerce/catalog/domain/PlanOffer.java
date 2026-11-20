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
public class PlanOffer {
    private String id;
    private String planVersionId;
    private String planId;
    private String priceId;
    private String marketCode;
    private LocalDateTime salesStart;
    private LocalDateTime salesEnd;
    private int displayOrder;
    private String status;
}
