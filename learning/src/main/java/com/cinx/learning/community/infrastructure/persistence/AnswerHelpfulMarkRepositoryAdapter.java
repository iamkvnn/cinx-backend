package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.learning.community.domain.AnswerHelpfulMark;
import com.cinx.learning.community.domain.AnswerHelpfulMarkRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AnswerHelpfulMarkRepositoryAdapter implements AnswerHelpfulMarkRepository {
    private final AnswerHelpfulMarkJpaRepository jpa;
    private final AnswerHelpfulMarkPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAnswerIdAndUserId(String answerId, String userId) {
        return jpa.existsByAnswerIdAndUserId(answerId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAnswerId(String answerId) {
        return jpa.countByAnswerId(answerId);
    }

    @Override
    @Transactional
    public void deleteByAnswerIdAndUserId(String answerId, String userId) {
        jpa.deleteByAnswerIdAndUserId(answerId, userId);
    }

    @Override
    @Transactional
    public void deleteByAnswerIdIn(Collection<String> answerIds) {
        jpa.deleteByAnswerIdIn(answerIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAnswerIdsByUserIdAndAnswerIdIn(String userId, Collection<String> answerIds) {
        return jpa.findAnswerIdsByUserIdAndAnswerIdIn(userId, answerIds);
    }

    @Override
    @Transactional
    public AnswerHelpfulMark save(AnswerHelpfulMark entity) {
        return store(entity);
    }

    /** Inserts a new AnswerHelpfulMark, or writes the changes onto the stored row and leaves the lock version to the store. */
    private AnswerHelpfulMark store(AnswerHelpfulMark entity) {
        AnswerHelpfulMarkJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
