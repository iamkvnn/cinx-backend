package com.cinx.learning.catalog.application.query;

import com.cinx.learning.shared.domain.LessonType;

import java.time.LocalDateTime;
import java.util.List;

public record LessonView (
        String id,
        String title,
        Long duration,
        LessonType lessonType,
        Integer orderIndex,
        Boolean isPreview,
        List<String> prerequisiteIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer videoDurationSeconds
) {
    public LessonView withVideoDurationSeconds(Integer seconds) {
        return new LessonView(id, title, duration, lessonType, orderIndex, isPreview, prerequisiteIds, createdAt, updatedAt, seconds);
    }
}

