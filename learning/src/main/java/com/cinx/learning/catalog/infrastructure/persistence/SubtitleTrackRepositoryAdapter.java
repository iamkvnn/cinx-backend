package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.catalog.domain.SubtitleTrackRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class SubtitleTrackRepositoryAdapter implements SubtitleTrackRepository {
    private final SubtitleTrackJpaRepository jpa;
    private final SubtitleTrackPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<SubtitleTrack> findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc(String lessonId) {
        return jpa.findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc(lessonId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubtitleTrack> findByIdAndVideoLessonLessonId(String id, String lessonId) {
        return jpa.findByIdAndVideoLessonLessonId(id, lessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubtitleTrack> findByVideoLessonLessonIdAndLanguageCode(String lessonId, String languageCode) {
        return jpa.findByVideoLessonLessonIdAndLanguageCode(lessonId, languageCode).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubtitleTrack> findByVideoLessonLessonIdAndIsDefaultTrue(String lessonId) {
        return jpa.findByVideoLessonLessonIdAndIsDefaultTrue(lessonId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByVideoLessonLessonId(String lessonId) {
        return jpa.countByVideoLessonLessonId(lessonId);
    }

    @Override
    @Transactional
    public void delete(SubtitleTrack entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<SubtitleTrack> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    public void flush() {
        jpa.flush();
    }

    @Override
    @Transactional
    public SubtitleTrack save(SubtitleTrack entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<SubtitleTrack> saveAll(Iterable<SubtitleTrack> entities) {
        List<SubtitleTrack> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new SubtitleTrack, or writes the changes onto the stored row and leaves the lock version to the store. */
    private SubtitleTrack store(SubtitleTrack entity) {
        SubtitleTrackJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
