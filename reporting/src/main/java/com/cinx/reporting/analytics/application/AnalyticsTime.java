package com.cinx.reporting.analytics.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Rows are UTC; days, as the admin reads them, are Vietnam days. */
public final class AnalyticsTime {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private AnalyticsTime() {
    }

    /** The UTC moment a Vietnam day starts. */
    public static LocalDateTime startOf(LocalDate day) {
        return day.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
