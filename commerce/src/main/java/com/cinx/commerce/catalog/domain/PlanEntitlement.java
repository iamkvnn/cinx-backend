package com.cinx.commerce.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanEntitlement {
    private String id;
    private String planVersionId;
    private String featureKey;
    private String grantType;
    private Boolean booleanValue;
    private Long quotaLimit;
    private Boolean quotaUnlimited;
    private String quotaUnit;
    private String resetPolicy;
}
