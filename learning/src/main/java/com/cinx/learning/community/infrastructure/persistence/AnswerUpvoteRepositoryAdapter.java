package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.learning.community.domain.AnswerUpvote;
import com.cinx.learning.community.domain.AnswerUpvoteRepository;
import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AnswerUpvoteRepositoryAdapter implements AnswerUpvoteRepository {
    private final AnswerUpvoteJpaRepository jpa;
    private final AnswerUpvotePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<AnswerUpvote> findByAnswerIdAndUserId(String answerId, String userId) {
        return jpa.findByAnswerIdAndUserId(answerId, userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAnswerIdAndUserId(String answerId, String userId) {
        return jpa.existsByAnswerIdAndUserId(answerId, userId);
    }

    @Override
    @Transactional
    public void deleteByAnswerIdIn(Collection<String> answerIds) {
        jpa.deleteByAnswerIdIn(answerIds);
    }

    @Override
    @Transactional
    public void delete(AnswerUpvote entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public AnswerUpvote save(AnswerUpvote entity) {
        return store(entity);
    }

    /** Inserts a new AnswerUpvote, or writes the changes onto the stored row and leaves the lock version to the store. */
    private AnswerUpvote store(AnswerUpvote entity) {
        AnswerUpvoteJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
