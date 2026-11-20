package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.RejectCourseReason;
import com.cinx.learning.catalog.domain.RejectCourseReasonRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class RejectCourseReasonRepositoryAdapter implements RejectCourseReasonRepository {
    private final RejectCourseReasonJpaRepository jpa;
    private final RejectCourseReasonPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<RejectCourseReason> findByCourse(String courseId) {
        return jpa.findByCourse(courseId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteForCourse(String courseId) {
        jpa.deleteForCourse(courseId);
    }

    @Override
    @Transactional
    public RejectCourseReason save(RejectCourseReason entity) {
        return store(entity);
    }

    /** Inserts a new RejectCourseReason, or writes the changes onto the stored row and leaves the lock version to the store. */
    private RejectCourseReason store(RejectCourseReason entity) {
        RejectCourseReasonJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
