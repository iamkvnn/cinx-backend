package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface VideoOptionJpaRepository extends JpaRepository<VideoOptionJpaEntity, String> {
}
