package com.cinx.learning.instructor.infrastructure.persistence;

import com.cinx.learning.instructor.domain.CommentTemplateKind;
import com.cinx.learning.instructor.domain.InstructorCommentTemplate;
import com.cinx.learning.instructor.domain.InstructorCommentTemplateRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class InstructorCommentTemplateRepositoryAdapter implements InstructorCommentTemplateRepository {
    private final InstructorCommentTemplateJpaRepository jpa;
    private final InstructorCommentTemplatePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<InstructorCommentTemplate> findAllByInstructorIdAndKindOrderByCreatedAtDesc(String instructorId, CommentTemplateKind kind) {
        return jpa.findAllByInstructorIdAndKindOrderByCreatedAtDesc(instructorId, kind).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByInstructorId(String instructorId) {
        return jpa.countByInstructorId(instructorId);
    }

    @Override
    @Transactional
    public void delete(InstructorCommentTemplate entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InstructorCommentTemplate> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public InstructorCommentTemplate save(InstructorCommentTemplate entity) {
        return store(entity);
    }

    /** Inserts a new InstructorCommentTemplate, or writes the changes onto the stored row and leaves the lock version to the store. */
    private InstructorCommentTemplate store(InstructorCommentTemplate entity) {
        InstructorCommentTemplateJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
