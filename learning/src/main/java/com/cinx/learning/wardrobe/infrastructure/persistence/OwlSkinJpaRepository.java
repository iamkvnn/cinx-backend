package com.cinx.learning.wardrobe.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface OwlSkinJpaRepository extends JpaRepository<OwlSkinJpaEntity, String> {
    List<OwlSkinJpaEntity> findAllByOrderBySortOrderAsc();

    Optional<OwlSkinJpaEntity> findByCode(String code);
}
