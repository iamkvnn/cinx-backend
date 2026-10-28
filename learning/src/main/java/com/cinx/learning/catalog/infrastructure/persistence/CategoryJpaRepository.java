package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, String> {
    Optional<CategoryJpaEntity> findByName(String name);
}

