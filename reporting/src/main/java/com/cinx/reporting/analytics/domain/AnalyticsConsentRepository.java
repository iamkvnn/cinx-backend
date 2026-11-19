package com.cinx.reporting.analytics.domain;

import java.time.LocalDateTime;

/** Reporting's copy of each learner's "Thống kê sử dụng" switch; no row means on. */
public interface AnalyticsConsentRepository {
    boolean allows(String userId);

    /** Keeps the value unless a newer one is already stored. */
    void record(String userId, boolean enabled, LocalDateTime updatedAt);
}
