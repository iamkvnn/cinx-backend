package com.cinx.learning.engagement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserExperienceJpaRepository extends JpaRepository<UserExperienceJpaEntity, String> {
}
