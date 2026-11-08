package com.cinx.commerce.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanPrice {
    private String id;
    private String planId;
    private String providerProductId;
    private String marketCode;
    private String currency;
    private String billingInterval;
    private int intervalCount;
    private BigDecimal amount;
    private String provider;
    private String providerAccountKey;
    private String providerPriceRef;
    private String providerLookupKey;
    private String taxBehavior;
    private String status;
}
