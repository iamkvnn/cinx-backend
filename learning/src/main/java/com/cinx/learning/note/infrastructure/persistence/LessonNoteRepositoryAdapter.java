package com.cinx.learning.note.infrastructure.persistence;

import com.cinx.learning.note.domain.LessonNote;
import com.cinx.learning.note.domain.LessonNoteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class LessonNoteRepositoryAdapter implements LessonNoteRepository {
    private final LessonNoteJpaRepository jpa;
    private final LessonNotePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<LessonNote> findByUserIdAndCourseIdAndLessonIdOrderByCreatedAtDesc(String userId, String courseId, String lessonId) {
        return jpa.findByUserIdAndCourseIdAndLessonIdOrderByCreatedAtDesc(userId, courseId, lessonId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonNote> findByUserIdAndCourseIdOrderByCreatedAtDesc(String userId, String courseId) {
        return jpa.findByUserIdAndCourseIdOrderByCreatedAtDesc(userId, courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void delete(LessonNote entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LessonNote> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public LessonNote save(LessonNote entity) {
        return store(entity);
    }

    /** Inserts a new LessonNote, or writes the changes onto the stored row and leaves the lock version to the store. */
    private LessonNote store(LessonNote entity) {
        LessonNoteJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
