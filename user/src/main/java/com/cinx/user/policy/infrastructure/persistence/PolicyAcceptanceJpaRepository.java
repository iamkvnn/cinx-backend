package com.cinx.user.policy.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface PolicyAcceptanceJpaRepository extends JpaRepository<PolicyAcceptanceJpaEntity, String> {
}
