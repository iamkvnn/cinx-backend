package com.cinx.learning.progress.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WatchedRangeTrackerTest {
    private final WatchedRangeTracker tracker = new WatchedRangeTracker(new ObjectMapper());

    @Test
    void malformedWatchedRangeJsonFallsBackToEmptyRanges() {
        assertThat(tracker.watchedSeconds("not-json")).isZero();
    }

    @Test
    void mergeCombinesOverlappingRanges() {
        String merged = tracker.merge("[[0,30],[40,50]]", 25, 45, 100);

        assertThat(tracker.watchedSeconds(merged)).isEqualTo(50);
    }
}
