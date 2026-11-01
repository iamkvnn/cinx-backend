package com.cinx.learning.engagement.infrastructure.persistence;

import com.cinx.learning.engagement.domain.UserExperience;
import com.cinx.learning.engagement.domain.UserExperienceRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class UserExperienceRepositoryAdapter implements UserExperienceRepository {
    private final UserExperienceJpaRepository jpa;
    private final UserExperiencePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserExperience> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public UserExperience save(UserExperience entity) {
        return store(entity);
    }

    /** Inserts a new UserExperience, or writes the changes onto the stored row and leaves the lock version to the store. */
    private UserExperience store(UserExperience entity) {
        UserExperienceJpaEntity stored = entity.getUserId() == null ? null : jpa.findById(entity.getUserId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
