package com.cinx.learning.instructor.api.dto;

import com.cinx.common.dto.PaginatedMetadata;

import java.time.LocalDateTime;
import java.util.List;

/** One page of a course's learners, with the segment counts. No email (operator: "Chỉ tên, không email"). */
public record CourseLearnersResponse(
        PaginatedMetadata page,
        List<Row> items,
        Counts counts,
        long newInLast30Days
) {
    public enum CertificateState { NONE, PENDING, ISSUED, REJECTED }

    public record Row(
            String userId,
            String name,
            String avatarUrl,
            LocalDateTime joinedAt,
            int progressPercent,
            int completedItems,
            int totalItems,
            Double avgScore,
            LocalDateTime lastActivityAt,
            long activeSeconds,
            CertificateState certificate,
            String certificateRequestId
    ) {
    }

    public record Counts(long all, long active, long completed, long inactive) {
    }
}
