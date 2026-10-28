package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.VideoOption;
import com.cinx.learning.catalog.domain.VideoOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class VideoOptionRepositoryAdapter implements VideoOptionRepository {
    private final VideoOptionJpaRepository jpa;
    private final VideoOptionPersistenceMapper mapper;

    @Override
    @Transactional
    public void deleteAll(Iterable<VideoOption> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    /** Inserts a new VideoOption, or writes the changes onto the stored row and leaves the lock version to the store. */
    private VideoOption store(VideoOption entity) {
        VideoOptionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
