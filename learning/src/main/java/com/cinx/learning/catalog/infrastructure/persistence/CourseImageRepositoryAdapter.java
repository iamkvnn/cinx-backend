package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseImage;
import com.cinx.learning.catalog.domain.CourseImageRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseImageRepositoryAdapter implements CourseImageRepository {
    private final CourseImageJpaRepository jpa;
    private final CourseImagePersistenceMapper mapper;

    @Override
    @Transactional
    public void delete(CourseImage entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseImage> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public CourseImage save(CourseImage entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<CourseImage> saveAll(Iterable<CourseImage> entities) {
        List<CourseImage> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new CourseImage, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseImage store(CourseImage entity) {
        CourseImageJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
