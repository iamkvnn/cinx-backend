package com.cinx.learning.community.application.query;

import java.util.List;
import java.util.Map;

public record ReportStatisticsOverviewView(
        Long reportsInRange,
        Map<String, Long> reportsByType,
        List<StatisticsByTimeView> reportsByTime,
        List<TopReportedRefView> topReportedRefs
) {
}
