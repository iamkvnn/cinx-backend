package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseReviewNote;
import com.cinx.learning.catalog.domain.CourseReviewNoteRepository;
import com.cinx.learning.shared.domain.ReviewNoteStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseReviewNoteRepositoryAdapter implements CourseReviewNoteRepository {
    private final CourseReviewNoteJpaRepository jpa;
    private final CourseReviewNotePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CourseReviewNote> findAllByCourseIdAndStatusOrderByCreatedAtAsc(String courseId, ReviewNoteStatus status) {
        return jpa.findAllByCourseIdAndStatusOrderByCreatedAtAsc(courseId, status).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseReviewNote> findAllByCourseId(String courseId) {
        return jpa.findAllByCourseId(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void delete(CourseReviewNote entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<CourseReviewNote> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseReviewNote> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public CourseReviewNote save(CourseReviewNote entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<CourseReviewNote> saveAll(Iterable<CourseReviewNote> entities) {
        List<CourseReviewNote> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new CourseReviewNote, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseReviewNote store(CourseReviewNote entity) {
        CourseReviewNoteJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
