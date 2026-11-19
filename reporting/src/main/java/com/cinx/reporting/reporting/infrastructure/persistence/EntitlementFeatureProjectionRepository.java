package com.cinx.reporting.reporting.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntitlementFeatureProjectionRepository extends JpaRepository<EntitlementFeatureProjection, Long> {
    void deleteAllByUserId(String userId);
}
