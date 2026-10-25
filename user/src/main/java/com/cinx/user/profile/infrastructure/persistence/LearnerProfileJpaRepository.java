package com.cinx.user.profile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface LearnerProfileJpaRepository extends JpaRepository<LearnerProfileJpaEntity, String> {
}
