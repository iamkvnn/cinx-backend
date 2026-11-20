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
public class CommerceFeatureRegistry {
    private String featureKey;
    private String ownerService;
    private String valueType;
    private String unit;
    private String resetPolicies;
    private boolean saleable;
    private boolean active;
}
