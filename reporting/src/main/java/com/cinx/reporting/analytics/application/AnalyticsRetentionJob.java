package com.cinx.reporting.analytics.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 02:00 Vietnam time, after the day has closed: daily counts, then the 180-day purge. */
@Component
@RequiredArgsConstructor
public class AnalyticsRetentionJob {
    private final AnalyticsRetentionService retention;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void run() {
        retention.run();
    }
}
