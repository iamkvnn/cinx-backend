package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.InVideoAssessmentSubmission;
import com.cinx.learning.assessment.domain.InVideoAssessmentSubmissionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class InVideoAssessmentSubmissionRepositoryAdapter implements InVideoAssessmentSubmissionRepository {
    private final InVideoAssessmentSubmissionJpaRepository jpa;
    private final InVideoAssessmentSubmissionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public long countByUserIdAndVideoLessonId(String userId, String videoLessonId) {
        return jpa.countByUserIdAndVideoLessonId(userId, videoLessonId);
    }

    @Override
    @Transactional
    public Optional<InVideoAssessmentSubmission> findForUpdateByUserIdAndVideoAssessmentId(String userId, String videoAssessmentId) {
        return jpa.findForUpdateByUserIdAndVideoAssessmentId(userId, videoAssessmentId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InVideoAssessmentSubmission> findByUserIdAndVideoLessonId(String userId, String videoLessonId) {
        return jpa.findByUserIdAndVideoLessonId(userId, videoLessonId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public InVideoAssessmentSubmission save(InVideoAssessmentSubmission entity) {
        return store(entity);
    }

    /** Inserts a new InVideoAssessmentSubmission, or writes the changes onto the stored row and leaves the lock version to the store. */
    private InVideoAssessmentSubmission store(InVideoAssessmentSubmission entity) {
        InVideoAssessmentSubmissionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
