package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.SubmissionLineComment;
import com.cinx.learning.assessment.domain.SubmissionLineCommentRepository;
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
class SubmissionLineCommentRepositoryAdapter implements SubmissionLineCommentRepository {
    private final SubmissionLineCommentJpaRepository jpa;
    private final SubmissionLineCommentPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionLineComment> findAllBySubmissionIdInOrderByLineAsc(Collection<String> submissionIds) {
        return jpa.findAllBySubmissionIdInOrderByLineAsc(submissionIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void delete(SubmissionLineComment entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubmissionLineComment> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public SubmissionLineComment save(SubmissionLineComment entity) {
        return store(entity);
    }

    /** Inserts a new SubmissionLineComment, or writes the changes onto the stored row and leaves the lock version to the store. */
    private SubmissionLineComment store(SubmissionLineComment entity) {
        SubmissionLineCommentJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
