package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.ArticleLesson;
import com.cinx.learning.catalog.domain.ArticleLessonRepository;
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
class ArticleLessonRepositoryAdapter implements ArticleLessonRepository {
    private final ArticleLessonJpaRepository jpa;
    private final ArticleLessonPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ArticleLesson> findByLessonId(String lessonId) {
        return jpa.findByLessonId(lessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<ArticleLesson> findAllByLessonIdIn(java.util.Collection<String> lessonIds) {
        return jpa.findAllByLessonIdIn(lessonIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void delete(ArticleLesson entity) {
        if (entity.getLessonId() != null) {
            jpa.deleteById(entity.getLessonId());
        }
    }

    @Override
    @Transactional
    public ArticleLesson save(ArticleLesson entity) {
        return store(entity);
    }

    /** Inserts a new ArticleLesson, or writes the changes onto the stored row and leaves the lock version to the store. */
    private ArticleLesson store(ArticleLesson entity) {
        ArticleLessonJpaEntity stored = entity.getLessonId() == null ? null : jpa.findById(entity.getLessonId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
