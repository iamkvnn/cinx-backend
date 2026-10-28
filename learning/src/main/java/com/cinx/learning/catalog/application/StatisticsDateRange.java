package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.domain.StatisticsGroupBy;

import java.time.LocalDateTime;
import java.util.List;

public record StatisticsDateRange(
        LocalDateTime start,
        LocalDateTime end,
        StatisticsGroupBy groupBy,
        List<String> bucketLabels
) {
    public boolean groupByDay() {
        return groupBy == StatisticsGroupBy.DAY;
    }
}

