package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.AssignmentCriterion;
import com.cinx.learning.assessment.domain.AssignmentCriterionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AssignmentCriterionRepositoryAdapter implements AssignmentCriterionRepository {
    private final AssignmentCriterionJpaRepository jpa;
    private final AssignmentCriterionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentCriterion> findAllByAssignmentIdOrderByOrderIndexAsc(String assignmentId) {
        return jpa.findAllByAssignmentIdOrderByOrderIndexAsc(assignmentId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<AssignmentCriterion> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional
    public List<AssignmentCriterion> saveAll(Iterable<AssignmentCriterion> entities) {
        List<AssignmentCriterion> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new AssignmentCriterion, or writes the changes onto the stored row and leaves the lock version to the store. */
    private AssignmentCriterion store(AssignmentCriterion entity) {
        AssignmentCriterionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
