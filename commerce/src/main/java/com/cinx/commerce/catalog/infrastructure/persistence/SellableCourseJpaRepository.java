package com.cinx.commerce.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SellableCourseJpaRepository extends JpaRepository<SellableCourseJpaEntity, String> {
}
