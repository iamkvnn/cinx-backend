package com.cinx.reporting.analytics.application;

import com.cinx.reporting.analytics.domain.ProductEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Rolls raw events into daily counts, then deletes raw events past {@link #RAW_DAYS}; daily counts stay. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsRetentionService {
    public static final int RAW_DAYS = 180;
    /** Days rolled up again each run, so a night the job did not run is still counted. */
    static final int ROLL_UP_DAYS = 3;
    static final int DELETE_CHUNK = 5_000;
    private static final int MAX_CHUNKS = 200;

    private final ProductEventRepository events;
    private final Clock clock;

    public void run() {
        LocalDate today = LocalDate.now(clock.withZone(AnalyticsTime.ZONE));
        for (int back = ROLL_UP_DAYS; back >= 1; back--) {
            LocalDate day = today.minusDays(back);
            events.rollUpDay(day, AnalyticsTime.startOf(day), AnalyticsTime.startOf(day.plusDays(1)));
        }
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(RAW_DAYS);
        int deleted = 0;
        for (int chunk = 0; chunk < MAX_CHUNKS; chunk++) {
            int gone = events.deleteOlderThan(cutoff, DELETE_CHUNK);
            deleted += gone;
            if (gone < DELETE_CHUNK) break;
        }
        log.info("Product events rolled up to {} and {} raw events older than {} days deleted", today.minusDays(1), deleted, RAW_DAYS);
    }
}
