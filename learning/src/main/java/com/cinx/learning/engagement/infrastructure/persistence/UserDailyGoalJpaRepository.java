package com.cinx.learning.engagement.infrastructure.persistence;

import com.cinx.learning.shared.domain.DailyGoalType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
interface UserDailyGoalJpaRepository extends JpaRepository<UserDailyGoalJpaEntity, String> {
    List<UserDailyGoalJpaEntity> findByUserIdAndGoalDate(String userId, LocalDate goalDate);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserDailyGoalJpaEntity> findByUserIdAndGoalDateAndGoalKey(String userId, LocalDate goalDate, String goalKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserDailyGoalJpaEntity> findByUserIdAndGoalDateAndGoalTypeAndTargetItemId(
            String userId,
            LocalDate goalDate,
            DailyGoalType goalType,
            String targetItemId);
    List<UserDailyGoalJpaEntity> findByUserIdAndGoalDateBetween(String userId, LocalDate startDate, LocalDate endDate);
    List<UserDailyGoalJpaEntity> findByGoalDateAndIsCompletedFalse(LocalDate goalDate);
}
