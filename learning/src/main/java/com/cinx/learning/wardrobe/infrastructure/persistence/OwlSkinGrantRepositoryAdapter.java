package com.cinx.learning.wardrobe.infrastructure.persistence;

import com.cinx.learning.wardrobe.domain.OwlSkinGrant;
import com.cinx.learning.wardrobe.domain.OwlSkinGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Grants are only ever added; the unique (user, skin) key keeps a repeated grant from doubling. */
@Component
@RequiredArgsConstructor
class OwlSkinGrantRepositoryAdapter implements OwlSkinGrantRepository {
    private final OwlSkinGrantJpaRepository jpa;
    private final OwlSkinGrantPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<OwlSkinGrant> findByUserId(String userId) {
        return jpa.findByUserId(userId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public OwlSkinGrant save(OwlSkinGrant entity) {
        return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
    }
}
