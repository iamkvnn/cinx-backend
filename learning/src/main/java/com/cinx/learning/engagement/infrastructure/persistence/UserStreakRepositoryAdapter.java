package com.cinx.learning.engagement.infrastructure.persistence;

import com.cinx.learning.engagement.domain.UserStreak;
import com.cinx.learning.engagement.domain.UserStreakRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class UserStreakRepositoryAdapter implements UserStreakRepository {
    private final UserStreakJpaRepository jpa;
    private final UserStreakPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserStreak> findByUserId(String userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findUserIdsWithStreakLastExtendedOn(int minStreak, LocalDate lastActivityDate) {
        return jpa.findUserIdsWithStreakLastExtendedOn(minStreak, lastActivityDate);
    }

    @Override
    @Transactional
    public Optional<UserStreak> findForUpdateByUserId(String userId) {
        return jpa.findForUpdateByUserId(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public UserStreak save(UserStreak entity) {
        return store(entity);
    }

    /** Inserts a new UserStreak, or writes the changes onto the stored row and leaves the lock version to the store. */
    private UserStreak store(UserStreak entity) {
        UserStreakJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
