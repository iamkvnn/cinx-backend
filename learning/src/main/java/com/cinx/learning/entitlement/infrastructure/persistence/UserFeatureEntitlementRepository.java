package com.cinx.learning.entitlement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserFeatureEntitlementRepository extends JpaRepository<UserFeatureEntitlement, UserFeatureEntitlement.Key> {
    Optional<UserFeatureEntitlement> findByUserIdAndFeatureKey(String userId, String featureKey);
    void deleteAllByUserId(String userId);
}
