package com.cinx.learning.engagement.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
interface UserStreakJpaRepository extends JpaRepository<UserStreakJpaEntity, String> {
    Optional<UserStreakJpaEntity> findByUserId(String userId);

    @Query("SELECT s.userId FROM UserStreak s WHERE s.currentStreak >= :minStreak AND s.lastActivityDate = :lastActivityDate")
    List<String> findUserIdsWithStreakLastExtendedOn(int minStreak, LocalDate lastActivityDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserStreak s WHERE s.userId = :userId")
    Optional<UserStreakJpaEntity> findForUpdateByUserId(String userId);
}
