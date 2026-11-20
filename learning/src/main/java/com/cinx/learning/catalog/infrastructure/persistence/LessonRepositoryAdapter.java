package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.LessonPlacement;
import com.cinx.learning.catalog.domain.LessonRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class LessonRepositoryAdapter implements LessonRepository {
    private final LessonJpaRepository jpa;
    private final LessonPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findReadableByStableIdIn(java.util.Collection<String> stableIds) {
        return jpa.findReadableByStableIdIn(stableIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Lesson> findBySectionAndStableId(String sectionId, String stableId) {
        return jpa.findBySectionAndStableId(sectionId, stableId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findByCourseAndStableId(String courseId, String stableId) {
        return jpa.findByCourseAndStableId(courseId, stableId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findReadableByCourseAndStableId(String courseId, String stableId) {
        return jpa.findReadableByCourseAndStableId(courseId, stableId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findPublishedByCourse(String courseId) {
        return jpa.findPublishedByCourse(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findPublishedCourseIdsByStableId(String stableId) {
        return jpa.findPublishedCourseIdsByStableId(stableId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonPlacement> findPlacementsByStableId(String stableId) {
        return jpa.findPlacementsByStableId(stableId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findEnrolledReadableByCourse(String courseId) {
        return jpa.findEnrolledReadableByCourse(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findEnrolledReadableByCourseIn(java.util.Collection<String> courseIds) {
        return jpa.findEnrolledReadableByCourseIn(courseIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findByDraft(String draftId) {
        return jpa.findByDraft(draftId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public List<Lesson> findBySectionIdsForUpdate(List<String> sectionIds) {
        return jpa.findBySectionIdsForUpdate(sectionIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public Optional<Lesson> findDraftLessonForUpdate(String draftId, String stableId) {
        return jpa.findDraftLessonForUpdate(draftId, stableId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccessibleByInstructor(String lessonId, String userId) {
        return jpa.isAccessibleByInstructor(lessonId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findDraftByDraft(String draftId) {
        return jpa.findDraftByDraft(draftId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void delete(Lesson entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<Lesson> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional
    public Lesson save(Lesson entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<Lesson> saveAll(Iterable<Lesson> entities) {
        List<Lesson> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new Lesson, or writes the changes onto the stored row and leaves the lock version to the store. */
    private Lesson store(Lesson entity) {
        LessonJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
