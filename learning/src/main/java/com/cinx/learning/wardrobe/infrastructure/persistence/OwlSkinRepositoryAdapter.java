package com.cinx.learning.wardrobe.infrastructure.persistence;

import com.cinx.learning.wardrobe.domain.OwlSkin;
import com.cinx.learning.wardrobe.domain.OwlSkinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class OwlSkinRepositoryAdapter implements OwlSkinRepository {
    private final OwlSkinJpaRepository jpa;
    private final OwlSkinPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<OwlSkin> findAllByOrderBySortOrderAsc() {
        return jpa.findAllByOrderBySortOrderAsc().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OwlSkin> findByCode(String code) {
        return jpa.findByCode(code).map(mapper::toDomain);
    }

    /** Inserts a new OwlSkin, or writes the changes onto the stored row and leaves the lock version to the store. */
    @Override
    @Transactional
    public OwlSkin save(OwlSkin entity) {
        OwlSkinJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
