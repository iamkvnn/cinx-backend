package com.cinx.commerce.subscription.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

interface CurrentSubscriptionPointerJpaRepository extends JpaRepository<CurrentSubscriptionPointerJpaEntity, CurrentSubscriptionPointerJpaEntity.Key> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CurrentSubscriptionPointerJpaEntity p where p.subjectId = :subjectId and p.subscriptionSlotCode = :slot")
    Optional<CurrentSubscriptionPointerJpaEntity> lockBySubjectIdAndSubscriptionSlotCode(String subjectId, String slot);
}
