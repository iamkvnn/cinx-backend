package com.cinx.commerce.catalog.infrastructure.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PlanVersionTranslationJpaRepository extends JpaRepository<PlanVersionTranslationJpaEntity, PlanVersionTranslationJpaEntity.Key> {
    Optional<PlanVersionTranslationJpaEntity> findByPlanVersionIdAndLocale(String planVersionId, String locale);
}
