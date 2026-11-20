package com.cinx.learning.community.application.query;

import com.cinx.learning.community.domain.ReportType;

import java.time.LocalDateTime;

public record AdminReportView(
        String id,
        String reporterId,
        UserSummaryView reporter,
        ReportType type,
        String reason,
        ReportedContentView reportedContent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
