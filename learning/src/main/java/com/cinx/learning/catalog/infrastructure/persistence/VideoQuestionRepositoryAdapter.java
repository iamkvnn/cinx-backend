package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.VideoOption;
import com.cinx.learning.catalog.domain.VideoQuestion;
import com.cinx.learning.catalog.domain.VideoQuestionRepository;
import com.cinx.learning.shared.infrastructure.persistence.ChildSync;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class VideoQuestionRepositoryAdapter implements VideoQuestionRepository {
    private final VideoQuestionJpaRepository jpa;
    private final VideoQuestionPersistenceMapper mapper;
    private final VideoOptionPersistenceMapper videoOptionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<VideoQuestion> findByVideoLessonLessonIdOrderByTimestampSecondsAsc(String lessonId) {
        return jpa.findByVideoLessonLessonIdOrderByTimestampSecondsAsc(lessonId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoQuestion> findByIdAndVideoLessonLessonId(String id, String lessonId) {
        return jpa.findByIdAndVideoLessonLessonId(id, lessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(VideoQuestion entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<VideoQuestion> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoQuestion> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public VideoQuestion save(VideoQuestion entity) {
        return store(entity);
    }

    /** Inserts a new VideoQuestion, or writes the changes onto the stored row and leaves the lock version to the store. */
    private VideoQuestion store(VideoQuestion entity) {
        VideoQuestionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        ChildSync.sync(stored.getOptions(), entity.getOptions(), VideoOptionJpaEntity::getId, VideoOption::getId, child -> {
            VideoOptionJpaEntity row = videoOptionMapper.toEntity(child);
            row.setVideoQuestion(stored);
            return row;
        });
        return mapper.toDomain(jpa.save(stored));
    }
}
