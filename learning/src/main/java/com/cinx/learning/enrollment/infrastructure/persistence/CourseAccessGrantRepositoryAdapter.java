package com.cinx.learning.enrollment.infrastructure.persistence;

import com.cinx.learning.enrollment.domain.CourseAccessGrant;
import com.cinx.learning.enrollment.domain.CourseAccessGrantRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseAccessGrantRepositoryAdapter implements CourseAccessGrantRepository {
    private final CourseAccessGrantJpaRepository jpa;
    private final CourseAccessGrantPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsActive(String userId, String courseId, String status, LocalDateTime now) {
        return jpa.existsActive(userId, courseId, status, now);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndCourseIdAndSourceAndSourceReference(String userId, String courseId, String source, String sourceReference) {
        return jpa.existsByUserIdAndCourseIdAndSourceAndSourceReference(userId, courseId, source, sourceReference);
    }

    @Override
    @Transactional
    public CourseAccessGrant save(CourseAccessGrant entity) {
        return store(entity);
    }

    /** Inserts a new CourseAccessGrant, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseAccessGrant store(CourseAccessGrant entity) {
        CourseAccessGrantJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
