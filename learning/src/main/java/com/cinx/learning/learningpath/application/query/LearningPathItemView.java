package com.cinx.learning.learningpath.application.query;

import com.cinx.learning.shared.domain.LessonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathItemView {
    private String id;
    private String courseId;
    private String lessonId;
    private Integer orderIndex;
    private String phase;
    private Boolean isSuggested;
    private Boolean isCompleted;

    // Filled by GET /paths/{id}, create and update; null in the list, which needs counts only.
    private String courseTitle;
    private String courseThumbnailUrl;
    private String lessonTitle;
    private LessonType lessonType;
    private Integer durationMinutes;
    private Boolean entitled;
}
