package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseEnrollmentEvent;
import com.cinx.learning.catalog.domain.CourseEnrollmentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseEnrollmentEventRepositoryAdapter implements CourseEnrollmentEventRepository {
    private final CourseEnrollmentEventJpaRepository jpa;
    private final CourseEnrollmentEventPersistenceMapper mapper;

    @Override
    @Transactional
    public CourseEnrollmentEvent saveAndFlush(CourseEnrollmentEvent entity) {
        CourseEnrollmentEvent saved = store(entity);
        jpa.flush();
        return saved;
    }

    /** Inserts a new CourseEnrollmentEvent, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseEnrollmentEvent store(CourseEnrollmentEvent entity) {
        CourseEnrollmentEventJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
