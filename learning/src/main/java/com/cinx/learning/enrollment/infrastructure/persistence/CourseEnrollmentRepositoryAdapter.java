package com.cinx.learning.enrollment.infrastructure.persistence;

import com.cinx.learning.enrollment.domain.CourseEnrollment;
import com.cinx.learning.enrollment.domain.CourseEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseEnrollmentRepositoryAdapter implements CourseEnrollmentRepository {
    private final CourseEnrollmentJpaRepository jpa;
    private final CourseEnrollmentPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndCourseIdAndStatus(String userId, String courseId, String status) {
        return jpa.existsByUserIdAndCourseIdAndStatus(userId, courseId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDistinctLearners() {
        return jpa.countDistinctLearners();
    }

    @Override
    @Transactional
    public CourseEnrollment save(CourseEnrollment entity) {
        return store(entity);
    }

    /** Inserts a new CourseEnrollment, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseEnrollment store(CourseEnrollment entity) {
        CourseEnrollmentJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
