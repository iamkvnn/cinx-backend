package com.cinx.commerce.catalog.infrastructure.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PlanPriceJpaRepository extends JpaRepository<PlanPriceJpaEntity, String> {
    Optional<PlanPriceJpaEntity> findByIdAndPlanId(String id, String planId);
}
