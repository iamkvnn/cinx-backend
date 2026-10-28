package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.Section;
import com.cinx.learning.catalog.domain.SectionRepository;
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
class SectionRepositoryAdapter implements SectionRepository {
    private final SectionJpaRepository jpa;
    private final SectionPersistenceMapper mapper;
    private final LessonPersistenceMapper lessonMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Section> findPublishedByCourse(String courseId) {
        return jpa.findPublishedByCourse(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Section> findDraftByDraft(String draftId) {
        return jpa.findDraftByDraft(draftId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public List<Section> findDraftByDraftForUpdate(String draftId) {
        return jpa.findDraftByDraftForUpdate(draftId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Section> findDraftSection(String draftId, String stableId) {
        return jpa.findDraftSection(draftId, stableId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(Section entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<Section> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Section> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Section> findAllById(Iterable<String> ids) {
        return jpa.findAllById(ids).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public Section save(Section entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<Section> saveAll(Iterable<Section> entities) {
        List<Section> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new Section, or writes the changes onto the stored row and leaves the lock version to the store. */
    private Section store(Section entity) {
        SectionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        ChildSync.sync(stored.getLessons(), entity.getLessons(), LessonJpaEntity::getId, Lesson::getId, child -> {
            LessonJpaEntity row = lessonMapper.toEntity(child);
            row.setSection(stored);
            return row;
        });
        return mapper.toDomain(jpa.save(stored));
    }
}
