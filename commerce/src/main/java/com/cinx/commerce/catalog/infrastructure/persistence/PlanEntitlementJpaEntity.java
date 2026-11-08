package com.cinx.commerce.catalog.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_entitlement")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlanEntitlementJpaEntity {
    @Id private String id;
    @Column(name = "plan_version_id", nullable = false) private String planVersionId;
    @Column(name = "feature_key", nullable = false) private String featureKey;
    @Column(name = "grant_type", nullable = false) private String grantType;
    @Column(name = "boolean_value") private Boolean booleanValue;
    @Column(name = "quota_limit") private Long quotaLimit;
    @Column(name = "quota_unlimited") private Boolean quotaUnlimited;
    @Column(name = "quota_unit") private String quotaUnit;
    @Column(name = "reset_policy") private String resetPolicy;
}
