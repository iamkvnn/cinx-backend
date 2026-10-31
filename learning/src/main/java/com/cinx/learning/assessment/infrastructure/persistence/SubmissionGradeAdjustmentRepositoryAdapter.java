package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.SubmissionGradeAdjustment;
import com.cinx.learning.assessment.domain.SubmissionGradeAdjustmentRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class SubmissionGradeAdjustmentRepositoryAdapter implements SubmissionGradeAdjustmentRepository {
    private final SubmissionGradeAdjustmentJpaRepository jpa;
    private final SubmissionGradeAdjustmentPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionGradeAdjustment> findAllBySubmissionIdIn(Collection<String> submissionIds) {
        return jpa.findAllBySubmissionIdIn(submissionIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void deleteAllBySubmissionId(String submissionId) {
        jpa.deleteAllBySubmissionId(submissionId);
    }

    @Override
    @Transactional
    public List<SubmissionGradeAdjustment> saveAll(Iterable<SubmissionGradeAdjustment> entities) {
        List<SubmissionGradeAdjustment> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new SubmissionGradeAdjustment, or writes the changes onto the stored row and leaves the lock version to the store. */
    private SubmissionGradeAdjustment store(SubmissionGradeAdjustment entity) {
        SubmissionGradeAdjustmentJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
