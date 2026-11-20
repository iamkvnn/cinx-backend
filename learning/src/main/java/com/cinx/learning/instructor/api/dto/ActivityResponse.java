package com.cinx.learning.instructor.api.dto;

import com.cinx.learning.shared.domain.ActivityAction;
import com.cinx.learning.shared.domain.InboxKind;

import java.time.Instant;

public record ActivityResponse(
        String id,
        InboxKind kind,
        ActivityAction action,
        String courseId,
        String courseTitle,
        String targetId,
        int itemCount,
        String summary,
        Instant createdAt
) {
}
