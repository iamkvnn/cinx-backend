package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.SubtitleJob;
import com.cinx.learning.catalog.domain.SubtitleJobRepository;
import com.cinx.learning.catalog.domain.SubtitleJobStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class SubtitleJobRepositoryAdapter implements SubtitleJobRepository {
    private final SubtitleJobJpaRepository jpa;
    private final SubtitleJobPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<SubtitleJob> findByVideoLessonLessonIdOrderByCreatedAtDesc(String lessonId) {
        return jpa.findByVideoLessonLessonIdOrderByCreatedAtDesc(lessonId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubtitleJob> findByIdAndVideoLessonLessonId(String id, String lessonId) {
        return jpa.findByIdAndVideoLessonLessonId(id, lessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByVideoLessonLessonIdAndTargetLanguageCodeAndStatusIn(String lessonId, String targetLanguageCode, List<SubtitleJobStatus> statuses) {
        return jpa.existsByVideoLessonLessonIdAndTargetLanguageCodeAndStatusIn(lessonId, targetLanguageCode, statuses);
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<SubtitleJob> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubtitleJob> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public SubtitleJob save(SubtitleJob entity) {
        return store(entity);
    }

    /** Inserts a new SubtitleJob, or writes the changes onto the stored row and leaves the lock version to the store. */
    private SubtitleJob store(SubtitleJob entity) {
        SubtitleJobJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
