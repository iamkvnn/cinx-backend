package com.cinx.notification.entitlement.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntitlementProjectionRepository extends JpaRepository<EntitlementProjection, String> {
}
