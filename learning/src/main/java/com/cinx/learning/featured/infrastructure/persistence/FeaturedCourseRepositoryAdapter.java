package com.cinx.learning.featured.infrastructure.persistence;

import com.cinx.learning.featured.domain.FeaturedCourse;
import com.cinx.learning.featured.domain.FeaturedCourseRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class FeaturedCourseRepositoryAdapter implements FeaturedCourseRepository {
    private final FeaturedCourseJpaRepository jpa;
    private final FeaturedCoursePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedCourse> findByActiveTrueOrderBySortOrderAsc() {
        return jpa.findByActiveTrueOrderBySortOrderAsc().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedCourse> findAllByOrderBySortOrderAsc() {
        return jpa.findAllByOrderBySortOrderAsc().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCourseId(String courseId) {
        return jpa.existsByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedCourse> findAllByCourseId(String courseId) {
        return jpa.findAllByCourseId(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public int maxSortOrder() {
        return jpa.maxSortOrder();
    }

    @Override
    @Transactional
    public void delete(FeaturedCourse entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<FeaturedCourse> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedCourse> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FeaturedCourse> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public FeaturedCourse save(FeaturedCourse entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<FeaturedCourse> saveAll(Iterable<FeaturedCourse> entities) {
        List<FeaturedCourse> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new FeaturedCourse, or writes the changes onto the stored row and leaves the lock version to the store. */
    private FeaturedCourse store(FeaturedCourse entity) {
        FeaturedCourseJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
