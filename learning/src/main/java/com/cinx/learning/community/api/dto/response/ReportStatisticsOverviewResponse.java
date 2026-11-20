package com.cinx.learning.community.api.dto.response;

import java.util.List;
import java.util.Map;

public record ReportStatisticsOverviewResponse(
        Long reportsInRange,
        Map<String, Long> reportsByType,
        List<StatisticsByTimeResponse> reportsByTime,
        List<TopReportedRefResponse> topReportedRefs
) {
}
