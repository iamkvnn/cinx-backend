package com.cinx.reporting.analytics.infrastructure.persistence;

import com.cinx.reporting.analytics.domain.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ExperimentJpaRepository extends JpaRepository<ExperimentJpaEntity, String> {
    List<ExperimentJpaEntity> findByStatus(ExperimentStatus status);
}
