package com.cinx.commerce.subscription.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

interface SubscriptionSubjectJpaRepository extends JpaRepository<SubscriptionSubjectJpaEntity, String> {
    Optional<SubscriptionSubjectJpaEntity> findBySubjectTypeAndSubjectReference(String subjectType, String subjectReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubscriptionSubjectJpaEntity s where s.subjectType = :subjectType and s.subjectReference = :subjectReference")
    Optional<SubscriptionSubjectJpaEntity> lockBySubjectTypeAndSubjectReference(String subjectType, String subjectReference);
}
