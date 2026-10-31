package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.AssignmentSubmissionAttachment;
import com.cinx.learning.assessment.domain.AssignmentSubmissionAttachmentRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AssignmentSubmissionAttachmentRepositoryAdapter implements AssignmentSubmissionAttachmentRepository {
    private final AssignmentSubmissionAttachmentJpaRepository jpa;
    private final AssignmentSubmissionAttachmentPersistenceMapper mapper;

    @Override
    @Transactional
    public List<AssignmentSubmissionAttachment> saveAll(Iterable<AssignmentSubmissionAttachment> entities) {
        List<AssignmentSubmissionAttachment> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new AssignmentSubmissionAttachment, or writes the changes onto the stored row and leaves the lock version to the store. */
    private AssignmentSubmissionAttachment store(AssignmentSubmissionAttachment entity) {
        AssignmentSubmissionAttachmentJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
