package com.cinx.commerce.catalog.infrastructure.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PlanOfferJpaRepository extends JpaRepository<PlanOfferJpaEntity, String> {
    List<PlanOfferJpaEntity> findAllByPlanVersionIdAndStatus(String planVersionId, String status);
}
