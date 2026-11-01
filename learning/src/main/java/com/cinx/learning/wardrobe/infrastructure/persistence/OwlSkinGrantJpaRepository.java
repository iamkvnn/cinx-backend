package com.cinx.learning.wardrobe.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface OwlSkinGrantJpaRepository extends JpaRepository<OwlSkinGrantJpaEntity, String> {
    List<OwlSkinGrantJpaEntity> findByUserId(String userId);
}
