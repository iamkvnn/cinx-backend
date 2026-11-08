package com.cinx.commerce.catalog.infrastructure.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface BillingPolicyVersionJpaRepository extends JpaRepository<BillingPolicyVersionJpaEntity, String> {
    Optional<BillingPolicyVersionJpaEntity> findByCodeAndVersionNumber(String code, long versionNumber);
}
