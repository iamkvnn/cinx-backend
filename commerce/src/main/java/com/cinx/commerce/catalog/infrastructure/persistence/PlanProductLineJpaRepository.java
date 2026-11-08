package com.cinx.commerce.catalog.infrastructure.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PlanProductLineJpaRepository extends JpaRepository<PlanProductLineJpaEntity, String> {
    Optional<PlanProductLineJpaEntity> findByCode(String code);
}
