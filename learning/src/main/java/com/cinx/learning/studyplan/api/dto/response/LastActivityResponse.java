package com.cinx.learning.studyplan.api.dto.response;

import com.cinx.learning.shared.domain.LessonType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Where "Học tiếp" goes: the last lesson touched, or the next one in its course when that lesson is done. */
public record LastActivityResponse(
        String courseId,
        String courseTitle,
        String lessonId,
        String lessonTitle,
        String sectionTitle,
        LessonType lessonType,
        @Schema(description = "Video resume position, seconds") Integer positionSeconds,
        @Schema(description = "Minutes left in this lesson (video: from its length and position)") Integer remainingMinutes,
        LocalDateTime lastActivityAt
) {
}
