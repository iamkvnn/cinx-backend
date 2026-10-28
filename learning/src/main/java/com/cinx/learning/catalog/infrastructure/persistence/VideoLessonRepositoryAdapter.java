package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.VideoLesson;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
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
class VideoLessonRepositoryAdapter implements VideoLessonRepository {
    private final VideoLessonJpaRepository jpa;
    private final VideoLessonPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoLesson> findByLessonId(String lessonId) {
        return jpa.findByLessonId(lessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoLesson> findAllByLessonIdIn(Collection<String> lessonIds) {
        return jpa.findAllByLessonIdIn(lessonIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void delete(VideoLesson entity) {
        if (entity.getLessonId() != null) {
            jpa.deleteById(entity.getLessonId());
        }
    }

    @Override
    @Transactional
    public VideoLesson save(VideoLesson entity) {
        return store(entity);
    }

    /** Inserts a new VideoLesson, or writes the changes onto the stored row and leaves the lock version to the store. */
    private VideoLesson store(VideoLesson entity) {
        VideoLessonJpaEntity stored = entity.getLessonId() == null ? null : jpa.findById(entity.getLessonId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
