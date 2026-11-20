package com.cinx.commerce.catalog.infrastructure.persistence;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "commerce_feature_registry")
@Getter
@Setter
@NoArgsConstructor
public class CommerceFeatureRegistryJpaEntity {
    @Id
    @Column(name = "feature_key")
    private String featureKey;
    @Column(name = "owner_service", nullable = false)
    private String ownerService;
    @Column(name = "value_type", nullable = false)
    private String valueType;
    private String unit;
    private String resetPolicies;
    @Column(nullable = false)
    private boolean saleable;
    @Column(nullable = false)
    private boolean active;
}
