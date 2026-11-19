package com.cinx.notification.notification.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

public record UserNotificationResponse(
        @Schema(example = "noti_123")
        String id,
        @Schema(example = "user_123")
        String userId,
        @Schema(example = "Course Enrolled Successfully")
        String title,
        @Schema(example = "You have successfully enrolled in Introduction to Java.")
        String message,
        @Schema(example = "COURSE_PUBLISHED")
        String type,
        @Schema(example = "course_123")
        String referenceId,
        @Schema(example = "/courses/course_123")
        String actionUrl,
        Map<String, Object> metadata,
        @Schema(example = "false")
        Boolean isRead,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime sentAt
) {
}
