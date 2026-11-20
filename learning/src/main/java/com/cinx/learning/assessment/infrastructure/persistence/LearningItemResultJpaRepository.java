package com.cinx.learning.assessment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface LearningItemResultJpaRepository extends JpaRepository<LearningItemResultJpaEntity, String> {
}
