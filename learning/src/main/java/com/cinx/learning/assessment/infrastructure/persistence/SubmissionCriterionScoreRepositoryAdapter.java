package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.SubmissionCriterionScore;
import com.cinx.learning.assessment.domain.SubmissionCriterionScoreRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class SubmissionCriterionScoreRepositoryAdapter implements SubmissionCriterionScoreRepository {
    private final SubmissionCriterionScoreJpaRepository jpa;
    private final SubmissionCriterionScorePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionCriterionScore> findAllBySubmissionIdIn(Collection<String> submissionIds) {
        return jpa.findAllBySubmissionIdIn(submissionIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void deleteAllBySubmissionId(String submissionId) {
        jpa.deleteAllBySubmissionId(submissionId);
    }

    @Override
    @Transactional
    public void deleteAllByCriterionIdIn(Collection<String> criterionIds) {
        jpa.deleteAllByCriterionIdIn(criterionIds);
    }

    @Override
    @Transactional
    public List<SubmissionCriterionScore> saveAll(Iterable<SubmissionCriterionScore> entities) {
        List<SubmissionCriterionScore> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new SubmissionCriterionScore, or writes the changes onto the stored row and leaves the lock version to the store. */
    private SubmissionCriterionScore store(SubmissionCriterionScore entity) {
        SubmissionCriterionScoreJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
