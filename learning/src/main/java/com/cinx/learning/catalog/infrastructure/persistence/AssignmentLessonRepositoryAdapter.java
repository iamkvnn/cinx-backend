package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.AssignmentAttachment;
import com.cinx.learning.catalog.domain.AssignmentLesson;
import com.cinx.learning.catalog.domain.AssignmentLessonRepository;
import com.cinx.learning.shared.infrastructure.persistence.ChildSync;
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
class AssignmentLessonRepositoryAdapter implements AssignmentLessonRepository {
    private final AssignmentLessonJpaRepository jpa;
    private final AssignmentLessonPersistenceMapper mapper;
    private final AssignmentAttachmentPersistenceMapper assignmentAttachmentMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentLesson> findByLessonId(String lessonId) {
        return jpa.findByLessonId(lessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<AssignmentLesson> findAllByLessonIdIn(java.util.Collection<String> lessonIds) {
        return jpa.findAllByLessonIdIn(lessonIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void delete(AssignmentLesson entity) {
        if (entity.getLessonId() != null) {
            jpa.deleteById(entity.getLessonId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentLesson> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public AssignmentLesson save(AssignmentLesson entity) {
        return store(entity);
    }

    /** Inserts a new AssignmentLesson, or writes the changes onto the stored row and leaves the lock version to the store. */
    private AssignmentLesson store(AssignmentLesson entity) {
        AssignmentLessonJpaEntity stored = entity.getLessonId() == null ? null : jpa.findById(entity.getLessonId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        ChildSync.sync(stored.getAttachments(), entity.getAttachments(), AssignmentAttachmentJpaEntity::getId, AssignmentAttachment::getId, child -> {
            AssignmentAttachmentJpaEntity row = assignmentAttachmentMapper.toEntity(child);
            row.setAssignmentLesson(stored);
            return row;
        });
        return mapper.toDomain(jpa.save(stored));
    }
}
