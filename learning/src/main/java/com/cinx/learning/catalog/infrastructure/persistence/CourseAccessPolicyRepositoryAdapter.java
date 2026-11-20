package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseAccessPolicy;
import com.cinx.learning.catalog.domain.CourseAccessPolicyRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseAccessPolicyRepositoryAdapter implements CourseAccessPolicyRepository {
    private final CourseAccessPolicyJpaRepository jpa;
    private final CourseAccessPolicyPersistenceMapper mapper;

    @Override
    @Transactional
    public void delete(CourseAccessPolicy entity) {
        if (entity.getCourseId() != null) {
            jpa.deleteById(entity.getCourseId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseAccessPolicy> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public CourseAccessPolicy save(CourseAccessPolicy entity) {
        return store(entity);
    }

    /** Inserts a new CourseAccessPolicy, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseAccessPolicy store(CourseAccessPolicy entity) {
        CourseAccessPolicyJpaEntity stored = entity.getCourseId() == null ? null : jpa.findById(entity.getCourseId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
