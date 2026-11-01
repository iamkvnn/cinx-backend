package com.cinx.learning.engagement.infrastructure.persistence;

import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.engagement.domain.UserDailyGoal;
import com.cinx.learning.engagement.domain.UserDailyGoalRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class UserDailyGoalRepositoryAdapter implements UserDailyGoalRepository {
    private final UserDailyGoalJpaRepository jpa;
    private final UserDailyGoalPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserDailyGoal> findByUserIdAndGoalDate(String userId, LocalDate goalDate) {
        return jpa.findByUserIdAndGoalDate(userId, goalDate).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public Optional<UserDailyGoal> findByUserIdAndGoalDateAndGoalKey(String userId, LocalDate goalDate, String goalKey) {
        return jpa.findByUserIdAndGoalDateAndGoalKey(userId, goalDate, goalKey).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<UserDailyGoal> findByUserIdAndGoalDateAndGoalTypeAndTargetItemId(String userId, LocalDate goalDate, DailyGoalType goalType, String targetItemId) {
        return jpa.findByUserIdAndGoalDateAndGoalTypeAndTargetItemId(userId, goalDate, goalType, targetItemId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDailyGoal> findByUserIdAndGoalDateBetween(String userId, LocalDate startDate, LocalDate endDate) {
        return jpa.findByUserIdAndGoalDateBetween(userId, startDate, endDate).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDailyGoal> findByGoalDateAndIsCompletedFalse(LocalDate goalDate) {
        return jpa.findByGoalDateAndIsCompletedFalse(goalDate).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void delete(UserDailyGoal entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public UserDailyGoal save(UserDailyGoal entity) {
        return store(entity);
    }

    /** Inserts a new UserDailyGoal, or writes the changes onto the stored row and leaves the lock version to the store. */
    private UserDailyGoal store(UserDailyGoal entity) {
        UserDailyGoalJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
