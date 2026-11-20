package com.cinx.learning.learningpath.infrastructure.persistence;

import com.cinx.learning.learningpath.domain.LearningPathItem;
import com.cinx.learning.shared.domain.LearningPathStatus;
import com.cinx.learning.learningpath.domain.UserLearningPath;
import com.cinx.learning.learningpath.domain.UserLearningPathRepository;
import com.cinx.learning.shared.infrastructure.persistence.ChildSync;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class UserLearningPathRepositoryAdapter implements UserLearningPathRepository {
    private final UserLearningPathJpaRepository jpa;
    private final UserLearningPathPersistenceMapper mapper;
    private final LearningPathItemPersistenceMapper learningPathItemMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserLearningPath> findByUserId(String userId) {
        return jpa.findByUserId(userId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserLearningPath> findByUserIdAndStatus(String userId, LearningPathStatus status) {
        return jpa.findByUserIdAndStatus(userId, status).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserLearningPath> findByUserIdAndStatusIn(String userId, List<LearningPathStatus> statuses) {
        return jpa.findByUserIdAndStatusIn(userId, statuses).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserLearningPath> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey) {
        return jpa.findByUserIdAndIdempotencyKey(userId, idempotencyKey).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserLearningPath> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public UserLearningPath save(UserLearningPath entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public UserLearningPath saveAndFlush(UserLearningPath entity) {
        UserLearningPath saved = store(entity);
        jpa.flush();
        return saved;
    }

    /** Inserts a new UserLearningPath, or writes the changes onto the stored row and leaves the lock version to the store. */
    private UserLearningPath store(UserLearningPath entity) {
        UserLearningPathJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        ChildSync.sync(stored.getItems(), entity.getItems(), LearningPathItemJpaEntity::getId, LearningPathItem::getId, child -> {
            LearningPathItemJpaEntity row = learningPathItemMapper.toEntity(child);
            row.setLearningPath(stored);
            return row;
        });
        return mapper.toDomain(jpa.save(stored));
    }
}
