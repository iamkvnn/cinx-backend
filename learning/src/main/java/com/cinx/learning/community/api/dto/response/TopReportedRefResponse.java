package com.cinx.learning.community.api.dto.response;

import com.cinx.learning.community.domain.ReportType;

public record TopReportedRefResponse(
        String refId,
        ReportType type,
        Long reportCount
) {
}
