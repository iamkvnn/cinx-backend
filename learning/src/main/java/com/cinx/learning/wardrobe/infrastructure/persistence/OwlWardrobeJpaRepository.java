package com.cinx.learning.wardrobe.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface OwlWardrobeJpaRepository extends JpaRepository<OwlWardrobeJpaEntity, String> {
    Optional<OwlWardrobeJpaEntity> findByUserId(String userId);
}
