package com.cinx.learning.studyplan.infrastructure.persistence;

import com.cinx.learning.studyplan.domain.StudyPlan;
import com.cinx.learning.studyplan.domain.StudyPlanRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class StudyPlanRepositoryAdapter implements StudyPlanRepository {
    private final StudyPlanJpaRepository jpa;
    private final StudyPlanPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<StudyPlan> findByUserId(String userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllUserIds() {
        return jpa.findAllUserIds();
    }

    @Override
    @Transactional
    public StudyPlan save(StudyPlan entity) {
        return store(entity);
    }

    /** Inserts a new StudyPlan, or writes the changes onto the stored row and leaves the lock version to the store. */
    private StudyPlan store(StudyPlan entity) {
        StudyPlanJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
