package com.cinx.notification.entitlement.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntitlementFeatureProjectionRepository extends JpaRepository<EntitlementFeatureProjection, Long> {
    void deleteAllByUserId(String userId);
}
