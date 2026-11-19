package com.cinx.reporting.analytics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface AnalyticsConsentJpaRepository extends JpaRepository<AnalyticsConsentJpaEntity, String> {
}
