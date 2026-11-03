package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.SellableCourse;
import com.cinx.commerce.catalog.domain.SellableCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SellableCourseRepositoryAdapter implements SellableCourseRepository {
    private final SellableCourseJpaRepository jpa;
    private final SellableCoursePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<SellableCourse> findById(String courseId) {
        return jpa.findById(courseId).map(mapper::toDomain);
    }

    /** Upserts by course id: a sellability event for a known course changes the stored row in place. */
    @Override
    @Transactional
    public SellableCourse save(SellableCourse course) {
        SellableCourseJpaEntity entity = jpa.findById(course.getCourseId()).orElse(null);
        if (entity == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(course)));
        }
        mapper.update(entity, course);
        return mapper.toDomain(jpa.save(entity));
    }
}
