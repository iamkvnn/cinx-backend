package com.cinx.commerce.catalog.infrastructure.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SubscriptionPlanVersionJpaRepository extends JpaRepository<SubscriptionPlanVersionJpaEntity, String> {
    List<SubscriptionPlanVersionJpaEntity> findAllByPublicationStatusOrderByPlanIdAscVersionNumberDesc(String status);
    List<SubscriptionPlanVersionJpaEntity> findAllByPlanIdOrderByVersionNumberDesc(String planId);
    Optional<SubscriptionPlanVersionJpaEntity> findByPlanIdAndVersionNumber(String planId, long versionNumber);
}
