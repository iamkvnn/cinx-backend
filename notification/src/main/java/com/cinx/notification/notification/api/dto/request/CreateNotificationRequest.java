package com.cinx.notification.notification.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record CreateNotificationRequest(
        @NotBlank
        @Schema(example = "New System Update")
        String title,
        @NotBlank
        @Schema(example = "The system will be under maintenance at 12 PM.")
        String message,
        @Schema(example = "COURSE_PUBLISHED")
        String type,
        @Schema(example = "course_123")
        String referenceId,
        @Schema(example = "/courses/course_123")
        String actionUrl,
        Map<String, Object> metadata,
        @NotEmpty
        List<String> userIds
) {
    public CreateNotificationRequest(String title, String message, List<String> userIds) {
        this(title, message, null, null, null, Map.of(), userIds);
    }
}
