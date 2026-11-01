package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.learning.community.domain.QuestionUpvote;
import com.cinx.learning.community.domain.QuestionUpvoteRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class QuestionUpvoteRepositoryAdapter implements QuestionUpvoteRepository {
    private final QuestionUpvoteJpaRepository jpa;
    private final QuestionUpvotePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionUpvote> findByQuestionIdAndUserId(String questionId, String userId) {
        return jpa.findByQuestionIdAndUserId(questionId, userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByQuestionIdAndUserId(String questionId, String userId) {
        return jpa.existsByQuestionIdAndUserId(questionId, userId);
    }

    @Override
    @Transactional
    public void deleteByQuestionId(String questionId) {
        jpa.deleteByQuestionId(questionId);
    }

    @Override
    @Transactional
    public void delete(QuestionUpvote entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public QuestionUpvote save(QuestionUpvote entity) {
        return store(entity);
    }

    /** Inserts a new QuestionUpvote, or writes the changes onto the stored row and leaves the lock version to the store. */
    private QuestionUpvote store(QuestionUpvote entity) {
        QuestionUpvoteJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
