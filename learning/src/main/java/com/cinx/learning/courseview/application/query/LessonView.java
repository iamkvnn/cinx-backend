package com.cinx.learning.courseview.application.query;

import com.cinx.learning.shared.domain.LessonType;

public record LessonView (
        String id,
        String title,
        Long duration,
        LessonType lessonType,
        Integer orderIndex
) {
}
