package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CatalogIdempotencyRecordJpaRepository extends JpaRepository<CatalogIdempotencyRecordJpaEntity, String> {
    Optional<CatalogIdempotencyRecordJpaEntity> findByUserIdAndIdemKey(String userId, String idemKey);
}
