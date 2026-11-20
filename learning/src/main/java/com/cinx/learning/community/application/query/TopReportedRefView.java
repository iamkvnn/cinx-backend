package com.cinx.learning.community.application.query;

import com.cinx.learning.community.domain.ReportType;

public record TopReportedRefView(
        String refId,
        ReportType type,
        Long reportCount
) {
}
