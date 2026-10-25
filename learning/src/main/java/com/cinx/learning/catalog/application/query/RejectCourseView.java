package com.cinx.learning.catalog.application.query;

import java.time.LocalDateTime;

public record RejectCourseView(
        String courseId,
        String reason,
        LocalDateTime rejectedAt
) {
}

