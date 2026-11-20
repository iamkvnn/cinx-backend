package com.cinx.commerce.catalog.infrastructure.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SubscriptionPlanJpaRepository extends JpaRepository<SubscriptionPlanJpaEntity, String> {
    Optional<SubscriptionPlanJpaEntity> findByCode(String code);
}
