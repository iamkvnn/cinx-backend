package com.cinx.commerce.catalog.infrastructure.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PlanEntitlementJpaRepository extends JpaRepository<PlanEntitlementJpaEntity, String> {
    List<PlanEntitlementJpaEntity> findAllByPlanVersionIdOrderByFeatureKey(String planVersionId);
}
