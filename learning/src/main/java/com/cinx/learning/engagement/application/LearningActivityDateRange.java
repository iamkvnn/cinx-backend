package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.domain.LearningActivityGroupBy;

import java.time.LocalDate;
import java.util.List;

public record LearningActivityDateRange(
        LocalDate startDate,
        LocalDate endDate,
        LearningActivityGroupBy groupBy,
        List<String> bucketLabels
) {
    public boolean groupByDay() {
        return groupBy == LearningActivityGroupBy.DAY;
    }
}
