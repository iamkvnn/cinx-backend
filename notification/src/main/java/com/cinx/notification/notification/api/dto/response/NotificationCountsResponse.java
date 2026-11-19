package com.cinx.notification.notification.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/** How many notifications the caller has, how many are unread, and how many of each type. */
public record NotificationCountsResponse(
        @Schema(description = "All of the caller's notifications", example = "47")
        long total,
        @Schema(description = "Those not yet read", example = "4")
        long unread,
        @Schema(description = "Notifications per type, read or not", example = "{\"COURSE_QUESTION_CREATED\": 19}")
        Map<String, Long> byType
) {
}
