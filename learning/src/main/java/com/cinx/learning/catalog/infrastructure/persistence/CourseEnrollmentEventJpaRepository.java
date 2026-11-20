package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface CourseEnrollmentEventJpaRepository extends JpaRepository<CourseEnrollmentEventJpaEntity, String> {
}

