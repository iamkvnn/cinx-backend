package com.cinx.learning.learningpath.api.dto.response;

import com.cinx.learning.shared.domain.LessonType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathItemResponse {
    @Schema(example = "path_item_123")
    private String id;
    @Schema(example = "course_123")
    private String courseId;
    @Schema(example = "lesson_123")
    private String lessonId;
    @Schema(example = "1")
    private Integer orderIndex;
    @Schema(description = "The named phase this lesson belongs to; null for a path without phases", example = "Nền tảng React")
    private String phase;
    @Schema(example = "true")
    private Boolean isSuggested;
    @Schema(example = "true")
    private Boolean isCompleted;

    // Filled by GET /paths/{id}, create and update; null in the list, which needs counts only.
    @Schema(example = "SQL & JPA thực chiến")
    private String courseTitle;
    @Schema(example = "https://cdn.example/course.png")
    private String courseThumbnailUrl;
    @Schema(example = "Quan hệ 1-n, n-n")
    private String lessonTitle;
    @Schema(example = "VIDEO")
    private LessonType lessonType;
    @Schema(description = "Estimated minutes: the video length, else the lesson type's default", example = "24")
    private Integer durationMinutes;
    @Schema(description = "Whether the learner already has access to the course", example = "true")
    private Boolean entitled;
}
