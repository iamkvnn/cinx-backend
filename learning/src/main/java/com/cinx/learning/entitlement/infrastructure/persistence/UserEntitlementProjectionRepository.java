package com.cinx.learning.entitlement.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

import java.util.Optional;

public interface UserEntitlementProjectionRepository extends JpaRepository<UserEntitlementProjection, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserEntitlementProjection p where p.userId = :userId")
    Optional<UserEntitlementProjection> lockByUserId(String userId);
}
