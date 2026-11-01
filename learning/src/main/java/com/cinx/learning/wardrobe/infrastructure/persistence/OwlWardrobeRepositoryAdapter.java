package com.cinx.learning.wardrobe.infrastructure.persistence;

import com.cinx.learning.wardrobe.domain.OwlWardrobe;
import com.cinx.learning.wardrobe.domain.OwlWardrobeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class OwlWardrobeRepositoryAdapter implements OwlWardrobeRepository {
    private final OwlWardrobeJpaRepository jpa;
    private final OwlWardrobePersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<OwlWardrobe> findByUserId(String userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    /** Inserts the learner's first wardrobe, or writes the changes onto the stored row. */
    @Override
    @Transactional
    public OwlWardrobe save(OwlWardrobe entity) {
        OwlWardrobeJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
