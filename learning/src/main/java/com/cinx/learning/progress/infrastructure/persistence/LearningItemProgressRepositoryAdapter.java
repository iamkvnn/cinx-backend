package com.cinx.learning.progress.infrastructure.persistence;

import com.cinx.learning.progress.domain.LearningItemProgress;
import com.cinx.learning.progress.domain.LearningItemProgressRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class LearningItemProgressRepositoryAdapter implements LearningItemProgressRepository {
    private final LearningItemProgressJpaRepository jpa;
    private final LearningItemProgressPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ItemCount> completedPerItem(String courseId) {
        return jpa.completedPerItem(courseId).stream().map(LearningItemProgressRepositoryAdapter::wrapItemCount).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassedRow> passedIn(String courseId, java.util.Collection<String> itemIds) {
        return jpa.passedIn(courseId, itemIds).stream().map(LearningItemProgressRepositoryAdapter::wrapPassedRow).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningItemProgress> findAllByUserIdAndCourseId(String userId, String courseId) {
        return jpa.findAllByUserIdAndCourseId(userId, courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LearningItemProgress> findByItemIdAndUserId(String itemId, String userId) {
        return jpa.findByItemIdAndUserId(itemId, userId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<LearningItemProgress> findByItemIdAndUserIdForUpdate(String itemId, String userId) {
        return jpa.findByItemIdAndUserIdForUpdate(itemId, userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningItemProgress> findAllByUserIdAndItemIdIn(String userId, List<String> itemIds) {
        return jpa.findAllByUserIdAndItemIdIn(userId, itemIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<LearningItemProgress> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional
    public LearningItemProgress save(LearningItemProgress entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<LearningItemProgress> saveAll(Iterable<LearningItemProgress> entities) {
        List<LearningItemProgress> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new LearningItemProgress, or writes the changes onto the stored row and leaves the lock version to the store. */
    private LearningItemProgress store(LearningItemProgress entity) {
        LearningItemProgressJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }

    private static LearningItemProgressRepository.ItemCount wrapItemCount(LearningItemProgressJpaRepository.ItemCount row) {
        if (row == null) {
            return null;
        }
        return new LearningItemProgressRepository.ItemCount() {
            @Override
            public String getItemId() {
                return row.getItemId();
            }
            @Override
            public long getTotal() {
                return row.getTotal();
            }
        };
    }

    private static LearningItemProgressRepository.PassedRow wrapPassedRow(LearningItemProgressJpaRepository.PassedRow row) {
        if (row == null) {
            return null;
        }
        return new LearningItemProgressRepository.PassedRow() {
            @Override
            public String getItemId() {
                return row.getItemId();
            }
            @Override
            public String getUserId() {
                return row.getUserId();
            }
        };
    }
}
