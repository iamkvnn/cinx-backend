package com.cinx.learning.community.api.dto.response;

import com.cinx.learning.community.domain.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AdminReportResponse(
        @Schema(example = "report_123")
        String id,
        @Schema(example = "user_123")
        String reporterId,
        UserSummaryResponse reporter,
        ReportType type,
        @Schema(example = "Contains inappropriate content.")
        String reason,
        ReportedContentResponse reportedContent,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime createdAt,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime updatedAt
) {
}
