package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.AssignmentAttachment;
import com.cinx.learning.catalog.domain.AssignmentAttachmentRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AssignmentAttachmentRepositoryAdapter implements AssignmentAttachmentRepository {
    private final AssignmentAttachmentJpaRepository jpa;
    private final AssignmentAttachmentPersistenceMapper mapper;

    @Override
    @Transactional
    public void deleteAll(Iterable<AssignmentAttachment> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional
    public List<AssignmentAttachment> saveAll(Iterable<AssignmentAttachment> entities) {
        List<AssignmentAttachment> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new AssignmentAttachment, or writes the changes onto the stored row and leaves the lock version to the store. */
    private AssignmentAttachment store(AssignmentAttachment entity) {
        AssignmentAttachmentJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
